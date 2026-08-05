package com.rupeex.main.notification.service;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.PaymentStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds email templates for payment notifications.
 * Provides HTML/text email bodies for various payment events.
 * 
 * Phase 2: Email Template Engine
 * Date: August 5, 2026
 */
@Service
public class PaymentEmailTemplateBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEmailTemplateBuilder.class);

    /**
     * Builds email template for successful payment (SETTLED status)
     */
    public String buildPaymentSuccessTemplate(Payment payment, String accountHolderName) {
        return String.format(
                "Dear %s,\n\n" +
                "Your payment has been successfully processed.\n\n" +
                "Payment Details:\n" +
                "- Reference: %s\n" +
                "- Amount: %s %s\n" +
                "- From Account: %s\n" +
                "- To Account: %s\n" +
                "- Status: SETTLED\n" +
                "- Timestamp: %s\n\n" +
                "Thank you for using RupeeX.\n\n" +
                "Best regards,\n" +
                "RupeeX Payment Platform",
                accountHolderName,
                payment.getPaymentReference(),
                payment.getAmount(),
                payment.getCurrency(),
                maskAccountNumber(payment.getSourceAccount()),
                maskAccountNumber(payment.getDestinationAccount()),
                payment.getUpdatedAt()
        );
    }

    /**
     * Builds email template for failed payment
     */
    public String buildPaymentFailureTemplate(Payment payment, String accountHolderName, String failureReason) {
        return String.format(
                "Dear %s,\n\n" +
                "Your payment could not be completed.\n\n" +
                "Failure Reason: %s (%s)\n\n" +
                "Payment Details:\n" +
                "- Reference: %s\n" +
                "- Amount: %s %s\n" +
                "- Attempted Time: %s\n\n" +
                "Next Steps:\n" +
                "- Check your account balance\n" +
                "- Verify recipient details\n" +
                "- Retry from your dashboard or contact support\n\n" +
                "Support: support@rupeex.com\n\n" +
                "Best regards,\n" +
                "RupeeX Support Team",
                accountHolderName,
                payment.getErrorCode(),
                failureReason,
                payment.getPaymentReference(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt()
        );
    }

    /**
     * Builds email template for debit notification (funds sent)
     */
    public String buildDebitNotificationTemplate(Payment payment, String accountHolderName) {
        return String.format(
                "Dear %s,\n\n" +
                "Funds have been deducted from your account.\n\n" +
                "Transaction Details:\n" +
                "- Amount: %s %s\n" +
                "- From Account: %s\n" +
                "- Payment Reference: %s\n" +
                "- Timestamp: %s\n\n" +
                "If you did not authorize this transaction, please contact support immediately.\n\n" +
                "Support: support@rupeex.com\n\n" +
                "Questions? We're here to help.\n\n" +
                "Best regards,\n" +
                "RupeeX Payment Platform",
                accountHolderName,
                payment.getAmount(),
                payment.getCurrency(),
                maskAccountNumber(payment.getSourceAccount()),
                payment.getPaymentReference(),
                payment.getUpdatedAt()
        );
    }

    /**
     * Builds email template for credit notification (funds received)
     */
    public String buildCreditNotificationTemplate(Payment payment, String accountHolderName) {
        return String.format(
                "Dear %s,\n\n" +
                "You have received a payment.\n\n" +
                "Transaction Details:\n" +
                "- Amount: %s %s\n" +
                "- To Account: %s\n" +
                "- From Account: %s\n" +
                "- Payment Reference: %s\n" +
                "- Timestamp: %s\n\n" +
                "This amount has been credited to your account.\n\n" +
                "Best regards,\n" +
                "RupeeX Payment Platform",
                accountHolderName,
                payment.getAmount(),
                payment.getCurrency(),
                maskAccountNumber(payment.getDestinationAccount()),
                maskAccountNumber(payment.getSourceAccount()),
                payment.getPaymentReference(),
                payment.getUpdatedAt()
        );
    }

    /**
     * Builds email subject line based on event type and payment status
     */
    public String buildSubject(String eventType, PaymentStatus status) {
        return switch(eventType) {
            case "PAYMENT_COMPLETED" -> "Payment Successful - Ref: [" + status + "]";
            case "PAYMENT_FAILED" -> "Payment Failed - Action Required";
            case "DEBIT_POSTED" -> "Funds Deducted from Your Account";
            case "CREDIT_POSTED" -> "Payment Received in Your Account";
            case "PAYMENT_RETRY" -> "Payment Retry Initiated";
            case "HIGH_RISK_PAYMENT" -> "Payment Requires Manual Review";
            case "PAYMENT_CANCELLED" -> "Payment Cancelled - Confirmation";
            default -> "RupeeX Payment Notification";
        };
    }

    /**
     * Masks account number for security in email display
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        String suffix = accountNumber.substring(accountNumber.length() - 4);
        return "****" + suffix;
    }
}

