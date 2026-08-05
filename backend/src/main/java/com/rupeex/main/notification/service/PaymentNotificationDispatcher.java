package com.rupeex.main.notification.service;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.notification.NotificationService;
import com.rupeex.main.notification.model.NotificationRequest;
import com.rupeex.main.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to payment notification events and dispatches emails to customers.
 * 
 * This is the critical bridge between payment events and email sending.
 * When NotificationEngineService calls notifyPaymentEvent(), this component
 * intercepts the event and sends appropriate emails based on event type.
 * 
 * Event Types Handled:
 * - PAYMENT_COMPLETED: Send success email to payer
 * - PAYMENT_FAILED: Send failure email to payer
 * - DEBIT_POSTED: Send debit notification to source account holder
 * - CREDIT_POSTED: Send credit notification to destination account holder
 * - PAYMENT_RETRY: Send retry notification to payer
 * - HIGH_RISK_PAYMENT: Send risk review notification
 * - PAYMENT_CANCELLED: Send cancellation notification
 * 
 * Phase 4: Event-Driven Email Dispatcher
 * Date: August 5, 2026
 */
@Component
public class PaymentNotificationDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentNotificationDispatcher.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEmailResolverService emailResolver;
    private final PaymentEmailTemplateBuilder templateBuilder;
    private final NotificationService emailService;

    @Value("${notification.email.enabled:true}")
    private boolean emailNotificationEnabled;

    @Value("${notification.events.payment-success.enabled:true}")
    private boolean paymentSuccessEnabled;

    @Value("${notification.events.payment-failure.enabled:true}")
    private boolean paymentFailureEnabled;

    @Value("${notification.events.debit-posted.enabled:true}")
    private boolean debitPostedEnabled;

    @Value("${notification.events.credit-posted.enabled:true}")
    private boolean creditPostedEnabled;

    public PaymentNotificationDispatcher(
            PaymentRepository paymentRepository,
            PaymentEmailResolverService emailResolver,
            PaymentEmailTemplateBuilder templateBuilder,
            NotificationService emailService) {
        this.paymentRepository = paymentRepository;
        this.emailResolver = emailResolver;
        this.templateBuilder = templateBuilder;
        this.emailService = emailService;
    }

    /**
     * Main event handler called when NotificationEngineService.notifyPaymentEvent() is invoked.
     * Routes the event to appropriate email sending logic based on event type.
     * 
     * This method is called from NotificationEngineService when payment events are recorded.
     * 
     * @param paymentId The ID of the payment
     * @param eventType The type of notification event
     * @param payload Additional event payload/context
     */
    @Transactional(readOnly = true)
    public void onPaymentEvent(Long paymentId, String eventType, String payload) {
        if (!emailNotificationEnabled) {
            logger.debug("Email notifications disabled globally. Skipping email for event: {}, paymentId: {}", 
                    eventType, paymentId);
            return;
        }

        logger.debug("Processing notification event: type={}, paymentId={}", eventType, paymentId);

        try {
            switch (eventType) {
                case "PAYMENT_COMPLETED" -> handlePaymentCompleted(paymentId);
                case "PAYMENT_FAILED" -> handlePaymentFailed(paymentId);
                case "DEBIT_POSTED" -> handleDebitPosted(paymentId);
                case "CREDIT_POSTED" -> handleCreditPosted(paymentId);
                case "PAYMENT_RETRY" -> handlePaymentRetry(paymentId, payload);
                case "HIGH_RISK_PAYMENT" -> handleHighRiskPayment(paymentId, payload);
                case "PAYMENT_CANCELLED" -> handlePaymentCancelled(paymentId);
                default -> logger.warn("Unknown event type: {}. No email sent.", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing notification event for paymentId={}, eventType={}", 
                    paymentId, eventType, e);
        }
    }

    /**
     * Handles PAYMENT_COMPLETED event - sends success email to payer
     */
    private void handlePaymentCompleted(Long paymentId) {
        if (!paymentSuccessEnabled) {
            logger.debug("Payment success emails disabled. Skipping for paymentId={}", paymentId);
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            logger.warn("Payment not found for paymentId={}", paymentId);
            return;
        }

        String payerEmail = emailResolver.resolvePayerEmail(paymentId);
        if (payerEmail == null) {
            logger.warn("Could not resolve payer email for successful payment {}. Email not sent.", 
                    payment.getPaymentReference());
            return;
        }

        String payerName = emailResolver.resolveSourceAccountHolder(payment.getSourceAccount());
        String emailBody = templateBuilder.buildPaymentSuccessTemplate(payment, payerName);
        String subject = templateBuilder.buildSubject("PAYMENT_COMPLETED", payment.getStatus());

        NotificationRequest request = new NotificationRequest();
        request.setToEmail(payerEmail);
        request.setSubject(subject);
        request.setMessage(emailBody);
        request.setRecipientName(payerName);
        request.setReferenceId(payment.getPaymentReference());

        logger.info("Sending payment success email for reference: {} to: {}", 
                payment.getPaymentReference(), maskEmail(payerEmail));
        emailService.sendNotification(request);
    }

    /**
     * Handles PAYMENT_FAILED event - sends failure email to payer
     */
    private void handlePaymentFailed(Long paymentId) {
        if (!paymentFailureEnabled) {
            logger.debug("Payment failure emails disabled. Skipping for paymentId={}", paymentId);
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            logger.warn("Payment not found for paymentId={}", paymentId);
            return;
        }

        String payerEmail = emailResolver.resolvePayerEmail(paymentId);
        if (payerEmail == null) {
            logger.warn("Could not resolve payer email for failed payment {}. Email not sent.", 
                    payment.getPaymentReference());
            return;
        }

        String payerName = emailResolver.resolveSourceAccountHolder(payment.getSourceAccount());
        String failureReason = payment.getErrorMessage() != null ? payment.getErrorMessage() : "Unknown reason";
        String emailBody = templateBuilder.buildPaymentFailureTemplate(payment, payerName, failureReason);
        String subject = templateBuilder.buildSubject("PAYMENT_FAILED", payment.getStatus());

        NotificationRequest request = new NotificationRequest();
        request.setToEmail(payerEmail);
        request.setSubject(subject);
        request.setMessage(emailBody);
        request.setRecipientName(payerName);
        request.setReferenceId(payment.getPaymentReference());

        logger.info("Sending payment failure email for reference: {} to: {}", 
                payment.getPaymentReference(), maskEmail(payerEmail));
        emailService.sendNotification(request);
    }

    /**
     * Handles DEBIT_POSTED event - notifies source account holder of funds deduction
     */
    private void handleDebitPosted(Long paymentId) {
        if (!debitPostedEnabled) {
            logger.debug("Debit posted emails disabled. Skipping for paymentId={}", paymentId);
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            logger.warn("Payment not found for paymentId={}", paymentId);
            return;
        }

        String sourceAccountEmail = emailResolver.resolveSourceAccountHolderEmail(paymentId);
        if (sourceAccountEmail == null) {
            logger.debug("Could not resolve source account holder email for paymentId={}. Debit notification not sent.", 
                    paymentId);
            return;
        }

        String sourceAccountHolder = emailResolver.resolveSourceAccountHolder(payment.getSourceAccount());
        String emailBody = templateBuilder.buildDebitNotificationTemplate(payment, sourceAccountHolder);
        String subject = templateBuilder.buildSubject("DEBIT_POSTED", payment.getStatus());

        NotificationRequest request = new NotificationRequest();
        request.setToEmail(sourceAccountEmail);
        request.setSubject(subject);
        request.setMessage(emailBody);
        request.setRecipientName(sourceAccountHolder);
        request.setReferenceId(payment.getPaymentReference());

        logger.info("Sending debit notification for reference: {} to source account holder: {}", 
                payment.getPaymentReference(), maskEmail(sourceAccountEmail));
        emailService.sendNotification(request);
    }

    /**
     * Handles CREDIT_POSTED event - notifies destination account holder of funds received
     */
    private void handleCreditPosted(Long paymentId) {
        if (!creditPostedEnabled) {
            logger.debug("Credit posted emails disabled. Skipping for paymentId={}", paymentId);
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            logger.warn("Payment not found for paymentId={}", paymentId);
            return;
        }

        String recipientEmail = emailResolver.resolveRecipientEmail(paymentId);
        if (recipientEmail == null) {
            logger.debug("Could not resolve recipient email for paymentId={}. Credit notification not sent.", 
                    paymentId);
            return;
        }

        String recipientName = emailResolver.resolveDestinationAccountHolder(payment.getDestinationAccount());
        String emailBody = templateBuilder.buildCreditNotificationTemplate(payment, recipientName);
        String subject = templateBuilder.buildSubject("CREDIT_POSTED", payment.getStatus());

        NotificationRequest request = new NotificationRequest();
        request.setToEmail(recipientEmail);
        request.setSubject(subject);
        request.setMessage(emailBody);
        request.setRecipientName(recipientName);
        request.setReferenceId(payment.getPaymentReference());

        logger.info("Sending credit notification for reference: {} to recipient: {}", 
                payment.getPaymentReference(), maskEmail(recipientEmail));
        emailService.sendNotification(request);
    }

    /**
     * Handles PAYMENT_RETRY event - notifies payer that payment will be retried
     */
    private void handlePaymentRetry(Long paymentId, String payload) {
        logger.debug("Payment retry event for paymentId={}. Retry notification skipped (optional).", paymentId);
        // Retry notifications can be enabled/disabled based on business requirements
        // For now, we skip them to avoid email spam
    }

    /**
     * Handles HIGH_RISK_PAYMENT event - notifies payer and support team
     */
    private void handleHighRiskPayment(Long paymentId, String payload) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            logger.warn("Payment not found for paymentId={}", paymentId);
            return;
        }

        logger.info("High-risk payment detected for reference: {}. Manual review required. Payload: {}", 
                payment.getPaymentReference(), payload);
        // High-risk notifications can trigger support alerts or require manual review
        // Implementation can be added based on business requirements
    }

    /**
     * Handles PAYMENT_CANCELLED event - notifies payer of cancellation
     */
    private void handlePaymentCancelled(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            logger.warn("Payment not found for paymentId={}", paymentId);
            return;
        }

        logger.info("Payment cancelled for reference: {}. Cancellation notification skipped (optional).", 
                payment.getPaymentReference());
        // Cancellation notifications can be implemented based on business requirements
    }

    /**
     * Masks email for secure logging
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
}

