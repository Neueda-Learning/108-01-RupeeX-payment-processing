package com.rupeex.main.platform.service;

import com.rupeex.main.entity.NotificationRecord;
import com.rupeex.main.repository.NotificationRecordRepository;
import com.rupeex.main.notification.service.PaymentNotificationDispatcher;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended NotificationEngineService with email dispatch integration.
 * 
 * Records notification events and emits system events, then delegates to
 * PaymentNotificationDispatcher to send emails based on event type.
 * 
 * Phase 5 Integration: Wires email dispatcher into event pipeline
 * Date: August 5, 2026
 */
@Service
public class NotificationEngineService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEngineService.class);

    private final NotificationRecordRepository notificationRecordRepository;
    private final SystemEventService systemEventService;
    
    @Autowired(required = false)
    private PaymentNotificationDispatcher paymentNotificationDispatcher;

    public NotificationEngineService(NotificationRecordRepository notificationRecordRepository,
                                     SystemEventService systemEventService) {
        this.notificationRecordRepository = notificationRecordRepository;
        this.systemEventService = systemEventService;
    }

    /**
     * Handles payment event notifications with email dispatch.
     * 
     * Flow:
     * 1. Save event to NotificationRecord (audit trail)
     * 2. Emit system event (WebSocket, if enabled)
     * 3. Dispatch email notification (via PaymentNotificationDispatcher)
     * 
     * @param paymentId The payment ID
     * @param type The event type (PAYMENT_COMPLETED, PAYMENT_FAILED, etc.)
     * @param payload Additional context for the event
     */
    public void notifyPaymentEvent(Long paymentId, String type, String payload) {
        // Step 1: Save notification record for audit trail
        NotificationRecord record = new NotificationRecord();
        record.setPaymentId(paymentId);
        record.setType(type);
        record.setPayload(payload != null ? payload : "");
        notificationRecordRepository.save(record);
        logger.debug("Notification event recorded: type={}, paymentId={}", type, paymentId);
        
        // Step 2: Emit system event (WebSocket)
        systemEventService.emit(type, paymentId, payload != null ? payload : "");
        logger.debug("System event emitted: type={}, paymentId={}", type, paymentId);
        
        // Step 3: Dispatch email notification (async)
        // PaymentNotificationDispatcher will handle email sending based on event type
        if (paymentNotificationDispatcher != null) {
            try {
                paymentNotificationDispatcher.onPaymentEvent(paymentId, type, payload);
                logger.debug("Email dispatch triggered for event: type={}, paymentId={}", type, paymentId);
            } catch (Exception e) {
                logger.error("Error dispatching email notification for paymentId={}, type={}", 
                        paymentId, type, e);
                // Continue processing even if email dispatch fails - payment should complete
            }
        } else {
            logger.warn("PaymentNotificationDispatcher not available. Emails will not be sent.");
        }
    }
}
