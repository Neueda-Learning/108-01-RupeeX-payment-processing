package com.rupeex.main.scheduler;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.service.PaymentProcessingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRetryScheduler Tests")
class PaymentRetrySchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessingService processingService;

    @InjectMocks
    private PaymentRetryScheduler paymentRetryScheduler;

    @Test
    @DisplayName("Should process all failed payments")
    void retryFailedPayments_WithFailedPayments_ProcessesAll() {
        Payment p1 = new Payment();
        ReflectionTestUtils.setField(p1, "id", 1L);
        Payment p2 = new Payment();
        ReflectionTestUtils.setField(p2, "id", 2L);

        when(paymentRepository.findByStatus(PaymentStatus.FAILED)).thenReturn(List.of(p1, p2));

        paymentRetryScheduler.retryFailedPayments();

        verify(processingService, times(1)).processPayment(1L);
        verify(processingService, times(1)).processPayment(2L);
    }

    @Test
    @DisplayName("Should do nothing when no failed payments")
    void retryFailedPayments_NoFailedPayments_NoProcessing() {
        when(paymentRepository.findByStatus(PaymentStatus.FAILED)).thenReturn(Collections.emptyList());

        paymentRetryScheduler.retryFailedPayments();

        verify(processingService, never()).processPayment(anyLong());
    }

    @Test
    @DisplayName("Should query only FAILED status payments")
    void retryFailedPayments_QueriesOnlyFailedStatus() {
        when(paymentRepository.findByStatus(PaymentStatus.FAILED)).thenReturn(Collections.emptyList());

        paymentRetryScheduler.retryFailedPayments();

        verify(paymentRepository, times(1)).findByStatus(PaymentStatus.FAILED);
        verify(paymentRepository, never()).findByStatus(PaymentStatus.CREATED);
    }
}
