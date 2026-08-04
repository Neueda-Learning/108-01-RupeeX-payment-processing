package com.rupeex.main.service.impl;

import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.repository.PaymentHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentAuditServiceImpl Tests")
class PaymentAuditServiceImplTest {

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    private PaymentAuditServiceImpl paymentAuditService;

    @BeforeEach
    void setUp() {
        paymentAuditService = new PaymentAuditServiceImpl();
        ReflectionTestUtils.setField(paymentAuditService, "repository", paymentHistoryRepository);
    }

    @Test
    @DisplayName("Should create payment history entry")
    void createHistory_ValidParams_Success() {
        // Given
        Long paymentId = 1L;
        PaymentStatus oldStatus = PaymentStatus.CREATED;
        PaymentStatus newStatus = PaymentStatus.VALIDATED;
        String reason = "Validation passed";

        // When
        paymentAuditService.createHistory(paymentId, oldStatus, newStatus, reason);

        // Then
        verify(paymentHistoryRepository, times(1)).save(any(PaymentHistory.class));
    }

    @Test
    @DisplayName("Should create history for CREATED to VALIDATED")
    void createHistory_CreatedToValidated_Success() {
        paymentAuditService.createHistory(100L, PaymentStatus.CREATED, PaymentStatus.VALIDATED, "Validation");
        verify(paymentHistoryRepository, times(1)).save(any(PaymentHistory.class));
    }

    @Test
    @DisplayName("Should create history for VALIDATED to SENT")
    void createHistory_ValidatedToSent_Success() {
        paymentAuditService.createHistory(101L, PaymentStatus.VALIDATED, PaymentStatus.SENT, "Sending");
        verify(paymentHistoryRepository, times(1)).save(any(PaymentHistory.class));
    }

    @Test
    @DisplayName("Should create history for SENT to COMPLETED")
    void createHistory_SentToCompleted_Success() {
        paymentAuditService.createHistory(102L, PaymentStatus.SENT, PaymentStatus.COMPLETED, "Completed");
        verify(paymentHistoryRepository, times(1)).save(any(PaymentHistory.class));
    }

    @Test
    @DisplayName("Should get history for payment")
    void getHistory_ValidPaymentId_ReturnsHistory() {
        // Given
        Long paymentId = 1L;
        List<PaymentHistory> expectedHistory = new ArrayList<>();

        // When
        List<PaymentHistory> result = paymentAuditService.getHistory(paymentId);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple history entries")
    void createHistory_MultipleEntries_Success() {
        Long paymentId = 1L;

        paymentAuditService.createHistory(paymentId, PaymentStatus.CREATED, PaymentStatus.VALIDATED, "Step 1");
        paymentAuditService.createHistory(paymentId, PaymentStatus.VALIDATED, PaymentStatus.SENT, "Step 2");
        paymentAuditService.createHistory(paymentId, PaymentStatus.SENT, PaymentStatus.COMPLETED, "Step 3");

        verify(paymentHistoryRepository, times(3)).save(any(PaymentHistory.class));
    }
}

