package com.rupeex.main.platform.service;

import com.rupeex.main.entity.DeadLetterQueueEntry;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.ProcessingQueueEntry;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.repository.AccountsRepository;
import com.rupeex.main.repository.DeadLetterQueueRepository;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.repository.ProcessingQueueRepository;
import com.rupeex.main.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class QueueProcessingScheduler {

    private final ProcessingQueueRepository processingQueueRepository;
    private final PaymentRepository paymentRepository;
    private final AccountsRepository accountsRepository;
    private final SettlementEngineService settlementEngineService;
    private final AuditEngineService auditEngineService;
    private final NotificationEngineService notificationEngineService;
    private final DeadLetterQueueRepository deadLetterQueueRepository;

    private final int maxRetries;
    private final long baseDelayMs;

    public QueueProcessingScheduler(ProcessingQueueRepository processingQueueRepository,
                                    PaymentRepository paymentRepository,
                                    AccountsRepository accountsRepository,
                                    SettlementEngineService settlementEngineService,
                                    AuditEngineService auditEngineService,
                                    NotificationEngineService notificationEngineService,
                                    DeadLetterQueueRepository deadLetterQueueRepository,
                                    @Value("${payment.processing.max-retries:3}") int maxRetries,
                                    @Value("${payment.processing.base-delay-ms:500}") long baseDelayMs) {
        this.processingQueueRepository = processingQueueRepository;
        this.paymentRepository = paymentRepository;
        this.accountsRepository = accountsRepository;
        this.settlementEngineService = settlementEngineService;
        this.auditEngineService = auditEngineService;
        this.notificationEngineService = notificationEngineService;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
    }

    @Scheduled(fixedDelayString = "${payment.processing.queue-poll-interval-ms:1000}")
    @Transactional
    public void processQueue() {
        List<ProcessingQueueEntry> entries = processingQueueRepository.findReadyEntries("READY", DateTimeUtil.nowIst());
        for (ProcessingQueueEntry entry : entries) {
            processSingleEntry(entry);
        }
    }

    private void processSingleEntry(ProcessingQueueEntry entry) {
        Payment payment = paymentRepository.findById(entry.getPaymentId()).orElse(null);
        if (payment == null || payment.getStatus() == PaymentStatus.CANCELLED) {
            processingQueueRepository.delete(entry);
            return;
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        auditEngineService.record(payment.getId(), "SettlementEngine", "Processing Started", PaymentStatus.QUEUED, PaymentStatus.PROCESSING, 0L, null);

        try {
            Thread.sleep(settlementEngineService.randomDelayMs());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }

        if (settlementEngineService.shouldFailThisAttempt()) {
            handleFailure(payment, entry);
            return;
        }

        payment.setStatus(PaymentStatus.SENT);
        paymentRepository.save(payment);
        auditEngineService.record(payment.getId(), "SettlementEngine", "Payment Sent", PaymentStatus.PROCESSING, PaymentStatus.SENT, 0L, "Sent to external network");

        // Debit source account — if insufficient funds, fail the payment
        int debited = accountsRepository.debitBalance(payment.getSourceAccount(), payment.getAmount());
        if (debited == 0) {
            payment.markAsFailed("INSUFFICIENT_FUNDS", "Insufficient funds in source account");
            paymentRepository.save(payment);
            auditEngineService.record(payment.getId(), "SettlementEngine", "Insufficient Funds", PaymentStatus.SENT, PaymentStatus.FAILED, 0L, "Balance too low");
            notificationEngineService.notifyPaymentEvent(payment.getId(), "PAYMENT_FAILED", "Insufficient funds");
            processingQueueRepository.delete(entry);
            return;
        }

        // Notify source account holder of debit
        notificationEngineService.notifyPaymentEvent(payment.getId(), "DEBIT_POSTED", "Funds debited from your account");

        // Credit destination account
        accountsRepository.creditBalance(payment.getDestinationAccount(), payment.getAmount());

        // Notify destination account holder of credit
        notificationEngineService.notifyPaymentEvent(payment.getId(), "CREDIT_POSTED", "Funds credited to your account");

        payment.setStatus(PaymentStatus.SETTLED);
        paymentRepository.save(payment);
        auditEngineService.record(payment.getId(), "SettlementEngine", "Settlement Complete", PaymentStatus.SENT, PaymentStatus.SETTLED, 0L, "Payment settled");
        notificationEngineService.notifyPaymentEvent(payment.getId(), "PAYMENT_COMPLETED", "Payment settled successfully");

        processingQueueRepository.delete(entry);
    }

    private void handleFailure(Payment payment, ProcessingQueueEntry entry) {
        int nextRetry = entry.getRetryCount() + 1;
        if (nextRetry > maxRetries) {
            payment.markAsFailed("RETRY_EXCEEDED", "Exceeded retry limit");
            paymentRepository.save(payment);

            DeadLetterQueueEntry dlq = new DeadLetterQueueEntry();
            dlq.setPaymentId(payment.getId());
            dlq.setReason("Exceeded retry limit");
            dlq.setLastRetryCount(entry.getRetryCount());
            deadLetterQueueRepository.save(dlq);

            auditEngineService.record(payment.getId(), "SettlementEngine", "Moved to DLQ", PaymentStatus.PROCESSING, PaymentStatus.FAILED, 0L, "Retries exhausted");
            notificationEngineService.notifyPaymentEvent(payment.getId(), "PAYMENT_FAILED", "Moved to dead letter queue");
            processingQueueRepository.delete(entry);
            return;
        }

        entry.setRetryCount(nextRetry);
        entry.setStatus("READY");
        entry.setNextAttemptAt(DateTimeUtil.nowIst().plusNanos(baseDelayMs * 1_000_000L * nextRetry));
        processingQueueRepository.save(entry);

        payment.setStatus(PaymentStatus.QUEUED);
        paymentRepository.save(payment);

        auditEngineService.record(payment.getId(), "SettlementEngine", "Retry", PaymentStatus.PROCESSING, PaymentStatus.QUEUED, 0L, payment.getErrorMessage());
        notificationEngineService.notifyPaymentEvent(payment.getId(), "PAYMENT_RETRY", payment.getErrorMessage());
    }
}
