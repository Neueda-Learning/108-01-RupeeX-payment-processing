package com.rupeex.main.platform.service;

import com.rupeex.main.entity.AuditLog;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditEngineService {

    private final AuditLogRepository auditLogRepository;

    public AuditEngineService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Long paymentId, String service, String action, PaymentStatus before, PaymentStatus after,
                       Long processingTimeMs, String reason) {
        AuditLog log = new AuditLog();
        log.setPaymentId(paymentId);
        log.setService(service);
        log.setAction(action);
        log.setBeforeState(before);
        log.setAfterState(after);
        log.setProcessingTimeMs(processingTimeMs);
        log.setReason(reason);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getPaymentAuditTrail(Long paymentId) {
        return auditLogRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId);
    }
}
