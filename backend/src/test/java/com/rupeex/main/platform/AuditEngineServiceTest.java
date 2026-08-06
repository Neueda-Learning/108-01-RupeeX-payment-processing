package com.rupeex.main.platform;

import com.rupeex.main.entity.AuditLog;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.platform.service.AuditEngineService;
import com.rupeex.main.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEngineService Tests")
class AuditEngineServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditEngineService auditEngineService;

    @Test
    @DisplayName("Should save audit log with correct fields")
    void record_SavesAuditLog() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditEngineService.record(1L, "PaymentEngine", "Payment Created",
                null, PaymentStatus.CREATED, 0L, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog log = captor.getValue();
        assertThat(log.getPaymentId()).isEqualTo(1L);
        assertThat(log.getService()).isEqualTo("PaymentEngine");
        assertThat(log.getAction()).isEqualTo("Payment Created");
        assertThat(log.getBeforeState()).isNull();
        assertThat(log.getAfterState()).isEqualTo(PaymentStatus.CREATED);
        assertThat(log.getProcessingTimeMs()).isZero();
    }

    @Test
    @DisplayName("Should save audit log with before and after state")
    void record_WithStateTransition_SavesCorrectly() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditEngineService.record(2L, "SettlementEngine", "Processing Started",
                PaymentStatus.QUEUED, PaymentStatus.PROCESSING, 100L, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog log = captor.getValue();
        assertThat(log.getBeforeState()).isEqualTo(PaymentStatus.QUEUED);
        assertThat(log.getAfterState()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(log.getProcessingTimeMs()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should retrieve audit trail for payment")
    void getPaymentAuditTrail_ReturnsList() {
        AuditLog log1 = new AuditLog();
        log1.setPaymentId(5L);
        AuditLog log2 = new AuditLog();
        log2.setPaymentId(5L);

        when(auditLogRepository.findByPaymentIdOrderByCreatedAtAsc(5L))
                .thenReturn(List.of(log1, log2));

        List<AuditLog> trail = auditEngineService.getPaymentAuditTrail(5L);

        assertThat(trail).hasSize(2);
        verify(auditLogRepository).findByPaymentIdOrderByCreatedAtAsc(5L);
    }

    @Test
    @DisplayName("Should save audit log with reason")
    void record_WithReason_SetsReason() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditEngineService.record(3L, "FraudEngine", "Fraud Blocked",
                PaymentStatus.FRAUD_CHECKED, PaymentStatus.FAILED, 50L, "Score too high");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("Score too high");
    }
}
