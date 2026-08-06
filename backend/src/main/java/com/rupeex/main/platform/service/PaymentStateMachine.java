package com.rupeex.main.platform.service;

import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.exception.InvalidPaymentException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = Map.of(
            PaymentStatus.CREATED, Set.of(PaymentStatus.VALIDATED, PaymentStatus.SCHEDULED, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.SCHEDULED, Set.of(PaymentStatus.VALIDATED, PaymentStatus.CANCELLED, PaymentStatus.FAILED),
            PaymentStatus.VALIDATED, Set.of(PaymentStatus.RISK_ANALYZED, PaymentStatus.FAILED),
            PaymentStatus.RISK_ANALYZED, Set.of(PaymentStatus.FRAUD_CHECKED, PaymentStatus.FAILED),
            PaymentStatus.FRAUD_CHECKED, Set.of(PaymentStatus.QUEUED, PaymentStatus.FAILED, PaymentStatus.PENDING_ADMIN_APPROVAL),
            PaymentStatus.PENDING_ADMIN_APPROVAL, Set.of(PaymentStatus.QUEUED, PaymentStatus.DECLINED, PaymentStatus.FAILED),
            PaymentStatus.QUEUED, Set.of(PaymentStatus.PROCESSING, PaymentStatus.CANCELLED, PaymentStatus.FAILED),
            PaymentStatus.PROCESSING, Set.of(PaymentStatus.SENT, PaymentStatus.FAILED),
            PaymentStatus.SENT, Set.of(PaymentStatus.SETTLED, PaymentStatus.FAILED)
    );

    public void assertTransition(PaymentStatus from, PaymentStatus to) {
        if (from == to) {
            return;
        }
        Set<PaymentStatus> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new InvalidPaymentException("Invalid transition from " + from + " to " + to);
        }
    }
}
