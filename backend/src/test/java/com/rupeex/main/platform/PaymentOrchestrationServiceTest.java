package com.rupeex.main.platform;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.RiskScore;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.enums.RiskCategory;
import com.rupeex.main.platform.dto.PaymentPlatformRequest;
import com.rupeex.main.platform.dto.PaymentPlatformResponse;
import com.rupeex.main.platform.service.*;
import com.rupeex.main.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentOrchestrationService Tests")
class PaymentOrchestrationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private FraudResultRepository fraudResultRepository;
    @Mock private RiskScoreRepository riskScoreRepository;
    @Mock private ProcessingQueueRepository processingQueueRepository;
    @Mock private DeadLetterQueueRepository deadLetterQueueRepository;
    @Mock private AccountsRepository accountsRepository;
    @Mock private PaymentStateMachine paymentStateMachine;
    @Mock private FraudDetectionEngineService fraudDetectionEngineService;
    @Mock private RiskScoringEngineService riskScoringEngineService;
    @Mock private AuditEngineService auditEngineService;
    @Mock private NotificationEngineService notificationEngineService;
    @Mock private SystemEventService systemEventService;

    @InjectMocks
    private PaymentOrchestrationService orchestrationService;

    private PaymentPlatformRequest request;
    private Payment savedPayment;
    private FraudEvaluationResult lowRiskEval;
    private RiskScore lowRiskScore;

    @BeforeEach
    void setUp() {
        request = new PaymentPlatformRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("INR");
        request.setSourceAccount("ACC-001");
        request.setDestinationAccount("ACC-002");
        request.setIdempotencyKey("idem-001");
        request.setOriginCountry("IN");
        request.setScheduledAt(null);

        savedPayment = new Payment();
        ReflectionTestUtils.setField(savedPayment, "id", 1L);
        savedPayment.setAmount(new BigDecimal("500.00"));
        savedPayment.setCurrency("INR");
        savedPayment.setSourceAccount("ACC-001");
        savedPayment.setDestinationAccount("ACC-002");
        savedPayment.setPaymentReference("EP-REF-001");
        savedPayment.setStatus(PaymentStatus.CREATED);

        lowRiskEval = new FraudEvaluationResult(10, Collections.emptyList(), "No triggered rules");
        lowRiskScore = new RiskScore();
        lowRiskScore.setScore(10);
        lowRiskScore.setCategory(RiskCategory.LOW);
        lowRiskScore.setDecision("Auto Process");
    }

    @Test
    @DisplayName("Should create payment and queue for processing when low risk")
    void createPayment_LowRisk_QueuesForProcessing() {
        when(paymentRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(fraudDetectionEngineService.evaluate(any(), any())).thenReturn(lowRiskEval);
        when(riskScoringEngineService.saveRiskScore(anyLong(), any())).thenReturn(lowRiskScore);
        when(processingQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentPlatformResponse response = orchestrationService.createPayment(request);

        assertThat(response).isNotNull();
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(fraudDetectionEngineService).evaluate(any(), any());
        verify(riskScoringEngineService).saveRiskScore(anyLong(), any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException on duplicate idempotency key")
    void createPayment_DuplicateIdempotencyKey_ThrowsException() {
        when(paymentRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.of(savedPayment));

        assertThatThrownBy(() -> orchestrationService.createPayment(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate idempotency key");
    }

    @Test
    @DisplayName("Should auto-reject payment when risk score > 100")
    void createPayment_HighRiskScore_AutoRejectsPayment() {
        FraudEvaluationResult highRiskEval = new FraudEvaluationResult(120, Collections.emptyList(), "Critical fraud rules;");
        RiskScore highRiskScore = new RiskScore();
        highRiskScore.setScore(120);
        highRiskScore.setCategory(RiskCategory.CRITICAL);
        highRiskScore.setDecision("Auto Reject");

        when(paymentRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(fraudDetectionEngineService.evaluate(any(), any())).thenReturn(highRiskEval);
        when(riskScoringEngineService.saveRiskScore(anyLong(), any())).thenReturn(highRiskScore);

        orchestrationService.createPayment(request);

        verify(notificationEngineService).notifyPaymentEvent(anyLong(), eq("PAYMENT_AUTO_REJECTED"), any());
    }

    @Test
    @DisplayName("Should set payment to PENDING_ADMIN_APPROVAL for score 80-100")
    void createPayment_HighRiskNeedsApproval_SetsAdminApprovalStatus() {
        FraudEvaluationResult medHighRiskEval = new FraudEvaluationResult(90, Collections.emptyList(), "Risk rules;");
        RiskScore adminApprovalScore = new RiskScore();
        adminApprovalScore.setScore(90);
        adminApprovalScore.setCategory(RiskCategory.CRITICAL);
        adminApprovalScore.setDecision("Admin Approval Required");

        when(paymentRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(fraudDetectionEngineService.evaluate(any(), any())).thenReturn(medHighRiskEval);
        when(riskScoringEngineService.saveRiskScore(anyLong(), any())).thenReturn(adminApprovalScore);

        orchestrationService.createPayment(request);

        verify(notificationEngineService).notifyPaymentEvent(anyLong(), eq("ADMIN_APPROVAL_REQUIRED"), any());
    }

    @Test
    @DisplayName("Should throw when getting non-existent payment")
    void getPayment_NotFound_ThrowsException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrationService.getPayment(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("processScheduledPayment should skip non-SCHEDULED payment")
    void processScheduledPayment_NotScheduledStatus_DoesNothing() {
        savedPayment.setStatus(PaymentStatus.CREATED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(savedPayment));

        orchestrationService.processScheduledPayment(1L);

        verify(fraudDetectionEngineService, never()).evaluate(any(), any());
    }

    @Test
    @DisplayName("processScheduledPayment should skip missing payment")
    void processScheduledPayment_PaymentNotFound_DoesNothing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        orchestrationService.processScheduledPayment(99L);

        verify(fraudDetectionEngineService, never()).evaluate(any(), any());
    }
}
