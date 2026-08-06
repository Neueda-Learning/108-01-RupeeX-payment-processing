package com.rupeex.main.platform.service;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Writes a single structured line to {@code logs/transactions.log} for every
 * transaction lifecycle event on the platform (created, validated, risk
 * scored, queued, settled, failed, cancelled, admin approved/declined, ...).
 * <p>
 * Logging happens through a dedicated SLF4J logger ("TRANSACTION_LOG") that
 * {@code logback-spring.xml} routes to its own rolling file via an
 * {@code AsyncAppender} — the actual disk write is performed on a background
 * logging thread so it never blocks the request/queue-processing thread that
 * reports the event.
 */
@Service
public class TransactionLogService {

    private static final Logger TX_LOG = LoggerFactory.getLogger("TRANSACTION_LOG");

    public void log(Payment payment, String service, String action, PaymentStatus before, PaymentStatus after, String reason) {
        log(payment.getId(), payment.getPaymentReference(), payment.getAmount(), payment.getCurrency(),
                payment.getSourceAccount(), payment.getDestinationAccount(), service, action, before, after, reason);
    }

    public void log(Long paymentId, String paymentReference, BigDecimal amount, String currency,
                     String sourceAccount, String destinationAccount, String service, String action,
                     PaymentStatus before, PaymentStatus after, String reason) {
        TX_LOG.info("paymentId={} reference={} service={} action=\"{}\" status={}->{} amount={} currency={} source={} destination={} reason=\"{}\"",
                paymentId,
                paymentReference,
                service,
                action,
                before,
                after,
                amount,
                currency,
                sourceAccount,
                destinationAccount,
                reason == null ? "-" : reason.replace('\n', ' '));
    }
}
