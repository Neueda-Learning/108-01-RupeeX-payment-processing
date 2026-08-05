package com.rupeex.main.platform.service;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.entity.ProcessingQueueEntry;
import com.rupeex.main.entity.RiskScore;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.platform.dto.PaymentPlatformRequest;
import com.rupeex.main.platform.dto.PaymentPlatformResponse;
import com.rupeex.main.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentOrchestrationService {

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final FraudResultRepository fraudResultRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final ProcessingQueueRepository processingQueueRepository;
    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final FraudDetectionEngineService fraudDetectionEngineService;
    private final RiskScoringEngineService riskScoringEngineService;
    private final AuditEngineService auditEngineService;
    private final NotificationEngineService notificationEngineService;
    private final SystemEventService systemEventService;

    public PaymentOrchestrationService(PaymentRepository paymentRepository,
                                                 PaymentHistoryRepository paymentHistoryRepository,
                                                 FraudResultRepository fraudResultRepository,
                                                 RiskScoreRepository riskScoreRepository,
                                                 ProcessingQueueRepository processingQueueRepository,
                                                 DeadLetterQueueRepository deadLetterQueueRepository,
                                                 PaymentStateMachine paymentStateMachine,
                                                 FraudDetectionEngineService fraudDetectionEngineService,
                                                 RiskScoringEngineService riskScoringEngineService,
                                                 AuditEngineService auditEngineService,
                                                 NotificationEngineService notificationEngineService,
                                                 SystemEventService systemEventService) {
        this.paymentRepository = paymentRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.fraudResultRepository = fraudResultRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.processingQueueRepository = processingQueueRepository;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
        this.paymentStateMachine = paymentStateMachine;
        this.fraudDetectionEngineService = fraudDetectionEngineService;
        this.riskScoringEngineService = riskScoringEngineService;
        this.auditEngineService = auditEngineService;
        this.notificationEngineService = notificationEngineService;
        this.systemEventService = systemEventService;
    }

    @Transactional
    public PaymentPlatformResponse createPayment(PaymentPlatformRequest request) {
        paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).ifPresent(existing -> {
            throw new IllegalStateException("Duplicate idempotency key: " + existing.getId());
        });

        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setSourceAccount(request.getSourceAccount());
        payment.setDestinationAccount(request.getDestinationAccount());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setPaymentReference("EP-" + UUID.randomUUID());
        payment.setStatus(PaymentStatus.CREATED);
        payment = paymentRepository.save(payment);
        auditEngineService.record(payment.getId(), "PaymentEngine", "Payment Created", null, PaymentStatus.CREATED, 0L, null);

        transition(payment, PaymentStatus.VALIDATED, "ValidationEngine", "Validation Completed", "Basic validation passed");

        FraudEvaluationResult fraudEval = fraudDetectionEngineService.evaluate(payment, request.getOriginCountry());
        transition(payment, PaymentStatus.RISK_ANALYZED, "RiskScoringEngine", "Risk Analyzed", fraudEval.explanation());

        RiskScore riskScore = riskScoringEngineService.saveRiskScore(payment.getId(), fraudEval);
        transition(payment, PaymentStatus.FRAUD_CHECKED, "FraudDetectionEngine", "Fraud Analyzed", fraudEval.explanation());

        // Handle different risk score scenarios
        if (riskScore.getScore() > 100) {
            // Auto-reject payments with score > 100
            payment.markAsFailed("RISK_SCORE_TOO_HIGH", "Payment automatically rejected due to risk score exceeding threshold (score: " + riskScore.getScore() + ")");
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            auditEngineService.record(payment.getId(), "RiskEngine", "Payment Auto-Rejected", PaymentStatus.FRAUD_CHECKED, PaymentStatus.FAILED, 0L, "Risk score > 100");
            notificationEngineService.notifyPaymentEvent(payment.getId(), "PAYMENT_AUTO_REJECTED", "Risk score: " + riskScore.getScore());
        } else if (riskScore.getScore() >= 80 && riskScore.getScore() <= 100) {
            // Require admin approval for scores 80-100
            transition(payment, PaymentStatus.PENDING_ADMIN_APPROVAL, "RiskEngine", "Admin Approval Required", "Risk score requires manual admin review (score: " + riskScore.getScore() + ")");
            notificationEngineService.notifyPaymentEvent(payment.getId(), "ADMIN_APPROVAL_REQUIRED", "Risk score: " + riskScore.getScore());
        } else {
            // Auto-process payments with score < 80
            transition(payment, PaymentStatus.QUEUED, "QueueManager", "Queued", "Queued for settlement processing");
            enqueueForProcessing(payment.getId());
        }

        systemEventService.emit("PAYMENT_CREATED", payment.getId(), payment.getPaymentReference());
        return toResponse(payment);
    }

    public Page<Payment> getPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    /**
     * Get all payments with risk scores and fraud results included.
     * Orders by newest first.
     */
    public List<PaymentPlatformResponse> getAllPaymentsWithRiskScores() {
        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc();
        return payments.stream()
                .map(this::toResponse)
                .toList();
    }

    public PaymentPlatformResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        return toResponse(payment);
    }

    @Transactional
    public PaymentPlatformResponse retryPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));

        processingQueueRepository.findByPaymentId(id).ifPresentOrElse(entry -> {
            entry.setStatus("READY");
            entry.setNextAttemptAt(LocalDateTime.now());
            processingQueueRepository.save(entry);
        }, () -> enqueueForProcessing(id));

        if (payment.getStatus() == PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.QUEUED);
            paymentRepository.save(payment);
        }

        deadLetterQueueRepository.findByPaymentId(id).ifPresent(deadLetterQueueRepository::delete);
        notificationEngineService.notifyPaymentEvent(id, "PAYMENT_RETRY_REQUESTED", "Retry requested from API");
        return toResponse(payment);
    }

    @Transactional
    public PaymentPlatformResponse cancelPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        transition(payment, PaymentStatus.CANCELLED, "PaymentEngine", "Payment Cancelled", "Cancelled by API request");
        processingQueueRepository.findByPaymentId(id).ifPresent(processingQueueRepository::delete);
        notificationEngineService.notifyPaymentEvent(id, "PAYMENT_CANCELLED", "Payment cancelled by user");
        return toResponse(payment);
    }

    public List<PaymentHistory> history(Long id) {
        return paymentHistoryRepository.findByPaymentIdOrderByChangedAtDesc(id);
    }

    @Transactional
    public PaymentPlatformResponse adminApprovePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        
        if (payment.getStatus() != PaymentStatus.PENDING_ADMIN_APPROVAL) {
            throw new IllegalStateException("Payment is not in PENDING_ADMIN_APPROVAL status");
        }
        
        // Approve and queue for processing
        transition(payment, PaymentStatus.QUEUED, "AdminReview", "Admin Approved", "Payment approved by administrator");
        enqueueForProcessing(payment.getId());
        notificationEngineService.notifyPaymentEvent(id, "PAYMENT_ADMIN_APPROVED", "Administrator approved the payment");
        
        return toResponse(payment);
    }

    @Transactional
    public PaymentPlatformResponse adminDeclinePayment(Long id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        
        if (payment.getStatus() != PaymentStatus.PENDING_ADMIN_APPROVAL) {
            throw new IllegalStateException("Payment is not in PENDING_ADMIN_APPROVAL status");
        }
        
        // Decline the payment
        payment.markAsFailed("ADMIN_DECLINED", reason != null ? reason : "Payment declined by administrator");
        payment.setStatus(PaymentStatus.DECLINED);
        paymentRepository.save(payment);
        
        PaymentHistory history = new PaymentHistory();
        history.setPaymentId(payment.getId());
        history.setOldStatus(PaymentStatus.PENDING_ADMIN_APPROVAL);
        history.setNewStatus(PaymentStatus.DECLINED);
        history.setReason(reason);
        paymentHistoryRepository.save(history);
        
        auditEngineService.record(payment.getId(), "AdminReview", "Admin Declined", PaymentStatus.PENDING_ADMIN_APPROVAL, PaymentStatus.DECLINED, 0L, reason);
        notificationEngineService.notifyPaymentEvent(id, "PAYMENT_ADMIN_DECLINED", reason);
        
        return toResponse(payment);
    }

    public List<PaymentPlatformResponse> getPendingAdminApprovalPayments() {
        List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.PENDING_ADMIN_APPROVAL);
        return payments.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Newest first
                .map(this::toResponse)
                .toList();
    }

    private void enqueueForProcessing(Long paymentId) {
        ProcessingQueueEntry entry = processingQueueRepository.findByPaymentId(paymentId).orElseGet(ProcessingQueueEntry::new);
        entry.setPaymentId(paymentId);
        entry.setStatus("READY");
        entry.setRetryCount(0);
        entry.setNextAttemptAt(LocalDateTime.now());
        processingQueueRepository.save(entry);
    }

    private void transition(Payment payment, PaymentStatus target, String service, String action, String reason) {
        PaymentStatus before = payment.getStatus();
        paymentStateMachine.assertTransition(before, target);
        payment.setStatus(target);
        paymentRepository.save(payment);

        PaymentHistory history = new PaymentHistory();
        history.setPaymentId(payment.getId());
        history.setOldStatus(before);
        history.setNewStatus(target);
        history.setReason(reason);
        paymentHistoryRepository.save(history);

        auditEngineService.record(payment.getId(), service, action, before, target, 0L, reason);
        systemEventService.emit(action.toUpperCase().replace(' ', '_'), payment.getId(), reason);
    }

    private PaymentPlatformResponse toResponse(Payment payment) {
        PaymentPlatformResponse response = new PaymentPlatformResponse();
        response.setPaymentId(payment.getId());
        response.setPaymentReference(payment.getPaymentReference());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setSourceAccount(payment.getSourceAccount());
        response.setDestinationAccount(payment.getDestinationAccount());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());
        response.setErrorMessage(payment.getErrorMessage());
        response.setRiskScore(riskScoreRepository.findByPaymentId(payment.getId()).orElse(null));
        response.setFraudResults(fraudResultRepository.findByPaymentId(payment.getId()));
        return response;
    }
}
