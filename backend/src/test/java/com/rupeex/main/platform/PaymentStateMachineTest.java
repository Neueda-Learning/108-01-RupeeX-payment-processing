package com.rupeex.main.platform;

import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.exception.InvalidPaymentException;
import com.rupeex.main.platform.service.PaymentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentStateMachine Tests")
class PaymentStateMachineTest {

    private PaymentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
    }

    @Test
    @DisplayName("CREATED -> VALIDATED is valid")
    void assertTransition_CreatedToValidated_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED));
    }

    @Test
    @DisplayName("CREATED -> SCHEDULED is valid")
    void assertTransition_CreatedToScheduled_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.CREATED, PaymentStatus.SCHEDULED));
    }

    @Test
    @DisplayName("CREATED -> FAILED is valid")
    void assertTransition_CreatedToFailed_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.CREATED, PaymentStatus.FAILED));
    }

    @Test
    @DisplayName("CREATED -> CANCELLED is valid")
    void assertTransition_CreatedToCancelled_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.CREATED, PaymentStatus.CANCELLED));
    }

    @Test
    @DisplayName("VALIDATED -> RISK_ANALYZED is valid")
    void assertTransition_ValidatedToRiskAnalyzed_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.VALIDATED, PaymentStatus.RISK_ANALYZED));
    }

    @Test
    @DisplayName("RISK_ANALYZED -> FRAUD_CHECKED is valid")
    void assertTransition_RiskAnalyzedToFraudChecked_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.RISK_ANALYZED, PaymentStatus.FRAUD_CHECKED));
    }

    @Test
    @DisplayName("FRAUD_CHECKED -> QUEUED is valid")
    void assertTransition_FraudCheckedToQueued_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.FRAUD_CHECKED, PaymentStatus.QUEUED));
    }

    @Test
    @DisplayName("FRAUD_CHECKED -> PENDING_ADMIN_APPROVAL is valid")
    void assertTransition_FraudCheckedToPendingAdminApproval_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.FRAUD_CHECKED, PaymentStatus.PENDING_ADMIN_APPROVAL));
    }

    @Test
    @DisplayName("QUEUED -> PROCESSING is valid")
    void assertTransition_QueuedToProcessing_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.QUEUED, PaymentStatus.PROCESSING));
    }

    @Test
    @DisplayName("PROCESSING -> SENT is valid")
    void assertTransition_ProcessingToSent_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.PROCESSING, PaymentStatus.SENT));
    }

    @Test
    @DisplayName("SENT -> SETTLED is valid")
    void assertTransition_SentToSettled_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.SENT, PaymentStatus.SETTLED));
    }

    @Test
    @DisplayName("Same status transition is always valid")
    void assertTransition_SameStatus_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.CREATED, PaymentStatus.CREATED));
    }

    @Test
    @DisplayName("CREATED -> SETTLED is invalid")
    void assertTransition_CreatedToSettled_ThrowsInvalidPaymentException() {
        assertThatThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.CREATED, PaymentStatus.SETTLED))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    @DisplayName("SETTLED -> CREATED is invalid (no back-transition)")
    void assertTransition_SettledToCreated_ThrowsInvalidPaymentException() {
        assertThatThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.SETTLED, PaymentStatus.CREATED))
                .isInstanceOf(InvalidPaymentException.class);
    }

    @Test
    @DisplayName("PROCESSING -> QUEUED is invalid")
    void assertTransition_ProcessingToQueued_ThrowsInvalidPaymentException() {
        assertThatThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.PROCESSING, PaymentStatus.QUEUED))
                .isInstanceOf(InvalidPaymentException.class);
    }

    @Test
    @DisplayName("PENDING_ADMIN_APPROVAL -> QUEUED is valid")
    void assertTransition_PendingAdminApprovalToQueued_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.PENDING_ADMIN_APPROVAL, PaymentStatus.QUEUED));
    }

    @Test
    @DisplayName("PENDING_ADMIN_APPROVAL -> DECLINED is valid")
    void assertTransition_PendingAdminApprovalToDeclined_NoException() {
        assertThatNoException().isThrownBy(
                () -> stateMachine.assertTransition(PaymentStatus.PENDING_ADMIN_APPROVAL, PaymentStatus.DECLINED));
    }
}
