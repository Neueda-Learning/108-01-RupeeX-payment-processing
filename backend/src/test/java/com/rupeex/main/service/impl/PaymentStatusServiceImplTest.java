package com.rupeex.main.service.impl;

import com.rupeex.main.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentStatusServiceImpl Tests")
class PaymentStatusServiceImplTest {

    private PaymentStatusServiceImpl paymentStatusService;

    @BeforeEach
    void setUp() {
        paymentStatusService = new PaymentStatusServiceImpl();
    }

    @Test
    @DisplayName("Should allow CREATED to VALIDATED transition")
    void isValidTransition_CreatedToValidated_ReturnsTrue() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED)).isTrue();
    }

    @Test
    @DisplayName("Should allow CREATED to FAILED transition")
    void isValidTransition_CreatedToFailed_ReturnsTrue() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.CREATED, PaymentStatus.FAILED)).isTrue();
    }

    @Test
    @DisplayName("Should deny CREATED to SENT transition")
    void isValidTransition_CreatedToSent_ReturnsFalse() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.CREATED, PaymentStatus.SENT)).isFalse();
    }

    @Test
    @DisplayName("Should allow VALIDATED to SENT transition")
    void isValidTransition_ValidatedToSent_ReturnsTrue() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.VALIDATED, PaymentStatus.SENT)).isTrue();
    }

    @Test
    @DisplayName("Should allow VALIDATED to FAILED transition")
    void isValidTransition_ValidatedToFailed_ReturnsTrue() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.VALIDATED, PaymentStatus.FAILED)).isTrue();
    }

    @Test
    @DisplayName("Should allow SENT to COMPLETED transition")
    void isValidTransition_SentToCompleted_ReturnsTrue() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("Should allow SENT to FAILED transition")
    void isValidTransition_SentToFailed_ReturnsTrue() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.SENT, PaymentStatus.FAILED)).isTrue();
    }

    @Test
    @DisplayName("Should deny transition from COMPLETED state")
    void isValidTransition_FromCompleted_ReturnsFalse() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.COMPLETED, PaymentStatus.SENT)).isFalse();
    }

    @Test
    @DisplayName("Should deny transition from FAILED state")
    void isValidTransition_FromFailed_ReturnsFalse() {
        assertThat(paymentStatusService.isValidTransition(PaymentStatus.FAILED, PaymentStatus.VALIDATED)).isFalse();
    }

    @Test
    @DisplayName("Should update status successfully")
    void updateStatus_ValidParams_Success() {
        paymentStatusService.updateStatus(1L, PaymentStatus.VALIDATED);
        // Should not throw exception
    }
}

