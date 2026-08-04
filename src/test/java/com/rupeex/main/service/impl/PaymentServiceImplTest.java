package com.rupeex.main.service.impl;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.dto.PaymentResponse;
import com.rupeex.main.dto.VerificationDecisionRequest;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.PaymentVerification;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.enums.VerificationStatus;
import com.rupeex.main.exception.InvalidPaymentException;
import com.rupeex.main.exception.PaymentNotFoundException;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.repository.PaymentVerificationRepository;
import com.rupeex.main.service.IdempotencyService;
import com.rupeex.main.service.PaymentValidationService;
import com.rupeex.main.service.VerificationNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Tests")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentValidationService validationService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private PaymentVerificationRepository paymentVerificationRepository;

    @Mock
    private VerificationNotificationService verificationNotificationService;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                validationService,
                idempotencyService,
                paymentVerificationRepository,
                verificationNotificationService
        );
    }

    // ======================= CREATE PAYMENT TESTS =======================

    @Test
    @DisplayName("Should successfully create payment with valid request")
    void createPayment_ValidRequest_Success() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        Payment savedPayment = createMockPayment(1L, PaymentStatus.COMPLETED);

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // When
        PaymentResponse response = paymentService.createPayment(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualTo(request.getAmount());
        assertThat(response.getCurrency()).isEqualTo(request.getCurrency());
        assertThat(response.getSourceAccount()).isEqualTo(request.getSourceAccount());
        assertThat(response.getDestinationAccount()).isEqualTo(request.getDestinationAccount());
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.isVerificationRequired()).isFalse();

        verify(idempotencyService, times(1)).checkDuplicate(request.getIdempotencyKey());
        verify(validationService, times(1)).validate(request);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw exception when validation fails")
    void createPayment_InvalidRequest_ThrowsException() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        doThrow(new IllegalArgumentException("Invalid amount"))
                .when(validationService).validate(request);

        // When & Then
        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid amount");

        verify(validationService, times(1)).validate(request);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw exception when idempotency key is duplicate")
    void createPayment_DuplicateIdempotencyKey_ThrowsException() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        doThrow(new IllegalArgumentException("Duplicate idempotency key"))
                .when(idempotencyService).checkDuplicate(request.getIdempotencyKey());

        // When & Then
        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate idempotency key");

        verify(idempotencyService, times(1)).checkDuplicate(request.getIdempotencyKey());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should create payment with minimal required fields")
    void createPayment_MinimalRequest_Success() {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setSourceAccount("ACC-001");
        request.setDestinationAccount("ACC-002");
        request.setIdempotencyKey(UUID.randomUUID().toString());

        Payment savedPayment = createMockPayment(2L, PaymentStatus.COMPLETED);

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // When
        PaymentResponse response = paymentService.createPayment(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(2L);
        assertThat(response.isVerificationRequired()).isFalse();
    }

    // ======================= GET PAYMENT TESTS =======================

    @Test
    @DisplayName("Should retrieve payment by ID successfully")
    void getPaymentById_ValidId_Success() {
        // Given
        Long paymentId = 1L;
        Payment payment = createMockPayment(paymentId, PaymentStatus.COMPLETED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When
        PaymentResponse response = paymentService.getPaymentById(paymentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getAmount()).isEqualTo(payment.getAmount());
        assertThat(response.getStatus()).isEqualTo(payment.getStatus());

        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void getPaymentById_InvalidId_ThrowsException() {
        // Given
        Long paymentId = 999L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found");

        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    @DisplayName("Should retrieve payment with verification details")
    void getPaymentById_WithPendingVerification_ReturnsVerificationToken() {
        // Given
        Long paymentId = 1L;
        Payment payment = createMockPayment(paymentId, PaymentStatus.COMPLETED);

        PaymentVerification verification = new PaymentVerification();
        verification.setPaymentId(paymentId);
        verification.setStatus(VerificationStatus.PENDING);
        verification.setVerificationToken("test-token-123");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentVerificationRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.of(verification));

        // When
        PaymentResponse response = paymentService.getPaymentById(paymentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isVerificationRequired()).isTrue();
        assertThat(response.getVerificationToken()).isEqualTo("test-token-123");

        verify(paymentVerificationRepository, times(1)).findByPaymentId(paymentId);
    }

    // ======================= PROCESS VERIFICATION TESTS =======================

    @Test
    @DisplayName("Should approve payment verification")
    void processVerificationDecision_Approved_Success() {
        // Given
        Long paymentId = 1L;
        String token = "valid-token-123";

        Payment payment = createMockPayment(paymentId, PaymentStatus.PENDING_VERIFICATION);

        PaymentVerification verification = new PaymentVerification();
        verification.setPaymentId(paymentId);
        verification.setStatus(VerificationStatus.PENDING);
        verification.setVerificationToken(token);
        verification.setCustomerId("ACC-001");

        VerificationDecisionRequest decisionRequest = new VerificationDecisionRequest();
        decisionRequest.setToken(token);
        decisionRequest.setApproved(true);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentVerificationRepository
                .findByPaymentIdAndVerificationTokenAndStatus(paymentId, token, VerificationStatus.PENDING))
                .thenReturn(Optional.of(verification));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        PaymentResponse response = paymentService.processVerificationDecision(paymentId, decisionRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.isVerificationRequired()).isFalse();

        verify(paymentRepository, times(1)).findById(paymentId);
        verify(paymentVerificationRepository, times(1))
                .findByPaymentIdAndVerificationTokenAndStatus(paymentId, token, VerificationStatus.PENDING);
        verify(paymentRepository, times(1)).save(payment);
        verify(paymentVerificationRepository, times(1)).save(verification);
    }

    @Test
    @DisplayName("Should decline payment verification")
    void processVerificationDecision_Declined_Success() {
        // Given
        Long paymentId = 1L;
        String token = "valid-token-456";

        Payment payment = createMockPayment(paymentId, PaymentStatus.PENDING_VERIFICATION);

        PaymentVerification verification = new PaymentVerification();
        verification.setPaymentId(paymentId);
        verification.setStatus(VerificationStatus.PENDING);
        verification.setVerificationToken(token);

        VerificationDecisionRequest decisionRequest = new VerificationDecisionRequest();
        decisionRequest.setToken(token);
        decisionRequest.setApproved(false);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentVerificationRepository
                .findByPaymentIdAndVerificationTokenAndStatus(paymentId, token, VerificationStatus.PENDING))
                .thenReturn(Optional.of(verification));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        PaymentResponse response = paymentService.processVerificationDecision(paymentId, decisionRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isVerificationRequired()).isFalse();

        verify(paymentRepository, times(1)).save(payment);
        verify(paymentVerificationRepository, times(1)).save(verification);
    }

    @Test
    @DisplayName("Should throw exception with invalid verification token")
    void processVerificationDecision_InvalidToken_ThrowsException() {
        // Given
        Long paymentId = 1L;
        String invalidToken = "invalid-token";

        Payment payment = createMockPayment(paymentId, PaymentStatus.COMPLETED);

        VerificationDecisionRequest decisionRequest = new VerificationDecisionRequest();
        decisionRequest.setToken(invalidToken);
        decisionRequest.setApproved(true);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentVerificationRepository
                .findByPaymentIdAndVerificationTokenAndStatus(paymentId, invalidToken, VerificationStatus.PENDING))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processVerificationDecision(paymentId, decisionRequest))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Invalid or expired verification token");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw exception when payment not found during verification")
    void processVerificationDecision_PaymentNotFound_ThrowsException() {
        // Given
        Long paymentId = 999L;
        VerificationDecisionRequest decisionRequest = new VerificationDecisionRequest();
        decisionRequest.setToken("token");
        decisionRequest.setApproved(true);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processVerificationDecision(paymentId, decisionRequest))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // ======================= UPDATE PAYMENT STATUS TESTS =======================

    @Test
    @DisplayName("Should update payment status successfully")
    void updatePaymentStatus_ValidId_Success() {
        // Given
        Long paymentId = 1L;
        Payment payment = createMockPayment(paymentId, PaymentStatus.COMPLETED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.DECLINED);

        // Then
        verify(paymentRepository, times(1)).findById(paymentId);
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("Should throw exception when updating status for non-existent payment")
    void updatePaymentStatus_InvalidId_ThrowsException() {
        // Given
        Long paymentId = 999L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(paymentId, PaymentStatus.DECLINED))
                .isInstanceOf(PaymentNotFoundException.class);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // ======================= HELPER METHODS =======================

    private PaymentRequest createValidPaymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("USD");
        request.setSourceAccount("ACC-SRC-001");
        request.setDestinationAccount("ACC-DST-001");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        request.setCustomerEmail("customer@example.com");
        return request;
    }

    private Payment createMockPayment(Long id, PaymentStatus status) {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(id);
        when(payment.getAmount()).thenReturn(new BigDecimal("1000.00"));
        when(payment.getCurrency()).thenReturn("USD");
        when(payment.getSourceAccount()).thenReturn("ACC-SRC-001");
        when(payment.getDestinationAccount()).thenReturn("ACC-DST-001");
        when(payment.getPaymentReference()).thenReturn("PAY-" + UUID.randomUUID());
        when(payment.getStatus()).thenReturn(status);
        return payment;
    }
}

