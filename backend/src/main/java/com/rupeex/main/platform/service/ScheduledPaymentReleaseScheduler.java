package com.rupeex.main.platform.service;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls for payments held in {@code SCHEDULED} status whose
 * {@link Payment#getScheduledAt()} (IST) has passed, and releases each one
 * into the standard fraud/risk/settlement pipeline via
 * {@link PaymentOrchestrationService#processScheduledPayment(Long)}.
 */
@Component
public class ScheduledPaymentReleaseScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentOrchestrationService orchestrationService;

    public ScheduledPaymentReleaseScheduler(PaymentRepository paymentRepository,
                                             PaymentOrchestrationService orchestrationService) {
        this.paymentRepository = paymentRepository;
        this.orchestrationService = orchestrationService;
    }

    @Scheduled(fixedDelayString = "${payment.scheduling.poll-interval-ms:30000}")
    public void releaseDuePayments() {
        List<Payment> due = paymentRepository.findByStatusAndScheduledAtLessThanEqual(
                PaymentStatus.SCHEDULED, DateTimeUtil.nowIst());
        for (Payment payment : due) {
            orchestrationService.processScheduledPayment(payment.getId());
        }
    }
}
