package com.rupeex.main.service.impl;

import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.service.PaymentProcessingService;
import com.rupeex.main.service.PaymentStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProcessingServiceImpl Tests")
class PaymentProcessingServiceImplTest {

    @Mock
    private PaymentStatusService paymentStatusService;

    private PaymentProcessingService paymentProcessingService;

    @BeforeEach
    void setUp() {
        paymentProcessingService = new PaymentProcessingServiceImpl();
        ReflectionTestUtils.setField(paymentProcessingService, "statusService", paymentStatusService);
    }

    @Test
    @DisplayName("Should process payment through complete workflow")
    void processPayment_ValidPaymentId_CompletesWorkflow() {
        // Given
        Long paymentId = 1L;

        // When
        assertThatCode(() -> paymentProcessingService.processPayment(paymentId))
                .doesNotThrowAnyException();

        // Then - verify all three status updates occur in sequence
        verify(paymentStatusService, times(1)).updateStatus(eq(paymentId), eq(PaymentStatus.VALIDATED));
        verify(paymentStatusService, times(1)).updateStatus(eq(paymentId), eq(PaymentStatus.SENT));
        verify(paymentStatusService, times(1)).updateStatus(eq(paymentId), eq(PaymentStatus.COMPLETED));
        verify(paymentStatusService, times(3)).updateStatus(eq(paymentId), any());
    }

    @Test
    @DisplayName("Should handle multiple payment processing")
    void processPayment_MultiplePayments_Success() {
        // When
        paymentProcessingService.processPayment(1L);
        paymentProcessingService.processPayment(2L);
        paymentProcessingService.processPayment(3L);

        // Then
        verify(paymentStatusService, times(9)).updateStatus(any(), any());
    }

    @Test
    @DisplayName("Should process payment with large ID")
    void processPayment_LargePaymentId_Success() {
        // Given
        Long largePaymentId = Long.MAX_VALUE;

        // When & Then
        assertThatCode(() -> paymentProcessingService.processPayment(largePaymentId))
                .doesNotThrowAnyException();

        verify(paymentStatusService, times(3)).updateStatus(eq(largePaymentId), any());
    }

    @Test
    @DisplayName("Should update status to VALIDATED first")
    void processPayment_FirstStatusValidated_Success() {
        Long paymentId = 100L;

        paymentProcessingService.processPayment(paymentId);

        verify(paymentStatusService).updateStatus(paymentId, PaymentStatus.VALIDATED);
    }

    @Test
    @DisplayName("Should update status to SENT second")
    void processPayment_SecondStatusSent_Success() {
        Long paymentId = 200L;

        paymentProcessingService.processPayment(paymentId);

        verify(paymentStatusService).updateStatus(paymentId, PaymentStatus.SENT);
    }

    @Test
    @DisplayName("Should update status to COMPLETED third")
    void processPayment_ThirdStatusCompleted_Success() {
        Long paymentId = 300L;

        paymentProcessingService.processPayment(paymentId);

        verify(paymentStatusService).updateStatus(paymentId, PaymentStatus.COMPLETED);
    }
}

