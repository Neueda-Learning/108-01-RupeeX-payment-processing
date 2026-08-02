package com.rupeex.main.service.impl;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.dto.PaymentResponse;
import com.rupeex.main.dto.VerificationDecisionRequest;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.PaymentVerification;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.enums.VerificationStatus;
import com.rupeex.main.exception.InvalidPaymentException;
import com.rupeex.main.exception.PaymentNotFoundException;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.repository.PaymentVerificationRepository;
import com.rupeex.main.service.IdempotencyService;
import com.rupeex.main.service.PaymentService;
import com.rupeex.main.service.PaymentValidationService;
import com.rupeex.main.service.TrustScoreService;
import com.rupeex.main.service.VerificationNotificationService;
import com.rupeex.main.service.model.TrustAssessmentResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentServiceImpl
        implements PaymentService {

    private static final double TRUST_APPROVAL_THRESHOLD = 0.75;

    private final PaymentRepository paymentRepository;
    private final PaymentValidationService validationService;
    private final IdempotencyService idempotencyService;
    private final TrustScoreService trustScoreService;
    private final PaymentVerificationRepository paymentVerificationRepository;
    private final VerificationNotificationService verificationNotificationService;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentValidationService validationService,
            IdempotencyService idempotencyService,
            TrustScoreService trustScoreService,
            PaymentVerificationRepository paymentVerificationRepository,
            VerificationNotificationService verificationNotificationService) {

        this.paymentRepository = paymentRepository;
        this.validationService = validationService;
        this.idempotencyService = idempotencyService;
        this.trustScoreService = trustScoreService;
        this.paymentVerificationRepository = paymentVerificationRepository;
        this.verificationNotificationService = verificationNotificationService;
    }



    @Override
    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest request){


        idempotencyService.checkDuplicate(
                request.getIdempotencyKey()
        );


        validationService.validate(request);


        TrustAssessmentResult assessment =
                trustScoreService.assessTrust(
                        request.getSourceAccount(),
                        request
                );


        Payment payment = new Payment();

        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setSourceAccount(request.getSourceAccount());
        payment.setDestinationAccount(request.getDestinationAccount());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setPaymentReference(
                "PAY-" + UUID.randomUUID()
        );


        boolean requiresVerification =
                assessment.hasTriggers()
                        && assessment.getTrustScore()
                        <= TRUST_APPROVAL_THRESHOLD;

        payment.setStatus(
                requiresVerification
                        ? PaymentStatus.PENDING_VERIFICATION
                        : PaymentStatus.COMPLETED
        );


        Payment saved =
                paymentRepository.save(payment);


        PaymentResponse response =
                mapToResponse(saved);

        response.setTrustScore(
                assessment.getTrustScore()
        );

        response.setVerificationRequired(
                requiresVerification
        );


        if(requiresVerification){

            PaymentVerification verification =
                    createVerification(saved, request, assessment);

            response.setVerificationToken(
                    verification.getVerificationToken()
            );
        }


        return response;

    }



    @Override
    public PaymentResponse getPaymentById(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId
                ));


        PaymentResponse response = mapToResponse(payment);


        paymentVerificationRepository.findByPaymentId(paymentId)
                .ifPresent(verification -> {
                    response.setTrustScore(
                            verification.getTrustScoreAtDecision()
                    );
                    response.setVerificationRequired(
                            verification.getStatus() == VerificationStatus.PENDING
                    );
                    if (verification.getStatus() == VerificationStatus.PENDING) {
                        response.setVerificationToken(
                                verification.getVerificationToken()
                        );
                    }
                });


        return response;
    }



    @Override
    @Transactional
    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId
                ));

        payment.setStatus(status);
        paymentRepository.save(payment);

    }



    @Override
    @Transactional
    public PaymentResponse processVerificationDecision(
            Long paymentId,
            VerificationDecisionRequest request
    ) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId
                ));


        PaymentVerification verification =
                paymentVerificationRepository
                        .findByPaymentIdAndVerificationTokenAndStatus(
                                paymentId,
                                request.getToken(),
                                VerificationStatus.PENDING
                        )
                        .orElseThrow(() -> new InvalidPaymentException(
                                "Invalid or expired verification token"
                        ));


        TrustAssessmentResult assessment = new TrustAssessmentResult(
                verification.getTrustScoreAtDecision(),
                verificationTriggeredCategories(verification)
        );


        if(request.isApproved()){

            payment.setStatus(PaymentStatus.COMPLETED);
            verification.setStatus(VerificationStatus.APPROVED);

        } else {

            payment.markAsFailed(
                    "VERIFICATION_DECLINED",
                    "Customer declined payment verification"
            );
            payment.setStatus(PaymentStatus.DECLINED);
            verification.setStatus(VerificationStatus.DECLINED);
        }


        trustScoreService.applyVerificationOutcome(
                verification.getCustomerId(),
                assessment,
                request.isApproved()
        );


        paymentRepository.save(payment);
        paymentVerificationRepository.save(verification);


        PaymentResponse response = mapToResponse(payment);
        response.setTrustScore(assessment.getTrustScore());
        response.setVerificationRequired(false);

        return response;
    }



    private PaymentVerification createVerification(
            Payment payment,
            PaymentRequest request,
            TrustAssessmentResult assessment) {

        PaymentVerification verification =
                new PaymentVerification();

        verification.setPaymentId(payment.getId());
        verification.setCustomerId(request.getSourceAccount());
        verification.setCustomerEmail(request.getCustomerEmail());
        verification.setVerificationToken(UUID.randomUUID().toString());
        verification.setTrustScoreAtDecision(assessment.getTrustScore());
        verification.setCurrencyChangeTriggered(
                assessment.isTriggered(
                        com.rupeex.main.enums.TrustCategory.CURRENCY_CHANGE
                )
        );
        verification.setLargePaymentTriggered(
                assessment.isTriggered(
                        com.rupeex.main.enums.TrustCategory.LARGE_PAYMENT
                )
        );
        verification.setRapidPaymentsTriggered(
                assessment.isTriggered(
                        com.rupeex.main.enums.TrustCategory.RAPID_PAYMENTS
                )
        );
        verification.setStatus(VerificationStatus.PENDING);


        PaymentVerification saved =
                paymentVerificationRepository.save(verification);

        verificationNotificationService.sendVerificationEmail(
                request.getCustomerEmail(),
                payment.getId(),
                saved.getVerificationToken(),
                assessment.getTrustScore()
        );

        return saved;
    }



    private java.util.Set<com.rupeex.main.enums.TrustCategory>
    verificationTriggeredCategories(
            PaymentVerification verification) {

        java.util.Set<com.rupeex.main.enums.TrustCategory> categories =
                java.util.EnumSet.noneOf(
                        com.rupeex.main.enums.TrustCategory.class
                );

        if (verification.isCurrencyChangeTriggered()) {
            categories.add(
                    com.rupeex.main.enums.TrustCategory.CURRENCY_CHANGE
            );
        }

        if (verification.isLargePaymentTriggered()) {
            categories.add(
                    com.rupeex.main.enums.TrustCategory.LARGE_PAYMENT
            );
        }

        if (verification.isRapidPaymentsTriggered()) {
            categories.add(
                    com.rupeex.main.enums.TrustCategory.RAPID_PAYMENTS
            );
        }

        return categories;
    }



    private PaymentResponse mapToResponse(Payment payment) {

        PaymentResponse response =
                new PaymentResponse();

        response.setPaymentId(payment.getId());
        response.setPaymentReference(payment.getPaymentReference());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setSourceAccount(payment.getSourceAccount());
        response.setDestinationAccount(payment.getDestinationAccount());
        response.setStatus(payment.getStatus());
        response.setErrorCode(payment.getErrorCode());
        response.setErrorMessage(payment.getErrorMessage());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());

        return response;
    }

}
