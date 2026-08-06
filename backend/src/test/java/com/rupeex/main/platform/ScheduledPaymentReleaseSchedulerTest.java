package com.rupeex.main.platform;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.platform.service.PaymentOrchestrationService;
import com.rupeex.main.platform.service.ScheduledPaymentReleaseScheduler;
import com.rupeex.main.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledPaymentReleaseScheduler Tests")
class ScheduledPaymentReleaseSchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentOrchestrationService orchestrationService;

    @InjectMocks
    private ScheduledPaymentReleaseScheduler scheduler;

    @Test
    @DisplayName("Should release all due scheduled payments")
    void releaseDuePayments_WithDuePayments_ProcessesEach() {
        Payment p1 = new Payment();
        ReflectionTestUtils.setField(p1, "id", 10L);
        Payment p2 = new Payment();
        ReflectionTestUtils.setField(p2, "id", 11L);

        when(paymentRepository.findByStatusAndScheduledAtLessThanEqual(
                eq(PaymentStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(p1, p2));

        scheduler.releaseDuePayments();

        verify(orchestrationService).processScheduledPayment(10L);
        verify(orchestrationService).processScheduledPayment(11L);
    }

    @Test
    @DisplayName("Should do nothing when no due payments")
    void releaseDuePayments_NoDuePayments_NoProcessing() {
        when(paymentRepository.findByStatusAndScheduledAtLessThanEqual(
                eq(PaymentStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.releaseDuePayments();

        verify(orchestrationService, never()).processScheduledPayment(anyLong());
    }

    @Test
    @DisplayName("Should query only SCHEDULED payments")
    void releaseDuePayments_QueriesOnlyScheduledStatus() {
        when(paymentRepository.findByStatusAndScheduledAtLessThanEqual(
                eq(PaymentStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.releaseDuePayments();

        verify(paymentRepository).findByStatusAndScheduledAtLessThanEqual(
                eq(PaymentStatus.SCHEDULED), any(LocalDateTime.class));
        verifyNoInteractions(orchestrationService);
    }
}
