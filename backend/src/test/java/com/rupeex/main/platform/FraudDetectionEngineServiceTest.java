package com.rupeex.main.platform;

import com.rupeex.main.entity.FraudResult;
import com.rupeex.main.entity.FraudRule;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.FraudRuleType;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.platform.service.FraudDetectionEngineService;
import com.rupeex.main.platform.service.FraudEvaluationResult;
import com.rupeex.main.repository.AccountsRepository;
import com.rupeex.main.repository.FraudResultRepository;
import com.rupeex.main.repository.FraudRuleRepository;
import com.rupeex.main.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionEngineService Tests")
class FraudDetectionEngineServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private FraudResultRepository fraudResultRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountsRepository accountsRepository;

    @InjectMocks
    private FraudDetectionEngineService fraudDetectionEngineService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", 1L);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setCurrency("INR");
        payment.setSourceAccount("ACC-001");
        payment.setDestinationAccount("ACC-002");
        payment.setStatus(PaymentStatus.VALIDATED);
    }

    @Test
    @DisplayName("Should return zero score when no active rules")
    void evaluate_NoActiveRules_ReturnsZeroScore() {
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(Collections.emptyList());

        FraudEvaluationResult result = fraudDetectionEngineService.evaluate(payment, "IN");

        assertThat(result.score()).isZero();
        assertThat(result.results()).isEmpty();
    }

    @Test
    @DisplayName("Should trigger LARGE_TRANSACTION rule when amount exceeds threshold")
    void evaluate_LargeTransaction_RuleTriggered() {
        payment.setAmount(new BigDecimal("15000.00"));

        FraudRule rule = buildRule(FraudRuleType.LARGE_TRANSACTION, 10000.0, 40, "Large txn");
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(fraudResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FraudEvaluationResult result = fraudDetectionEngineService.evaluate(payment, "IN");

        assertThat(result.score()).isEqualTo(40);
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).isTriggered()).isTrue();
    }

    @Test
    @DisplayName("Should NOT trigger LARGE_TRANSACTION rule when amount below threshold")
    void evaluate_LargeTransaction_RuleNotTriggered() {
        payment.setAmount(new BigDecimal("500.00"));

        FraudRule rule = buildRule(FraudRuleType.LARGE_TRANSACTION, 10000.0, 40, "Large txn");
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(fraudResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FraudEvaluationResult result = fraudDetectionEngineService.evaluate(payment, "IN");

        assertThat(result.score()).isZero();
        assertThat(result.results().get(0).isTriggered()).isFalse();
    }

    @Test
    @DisplayName("Should trigger BLACKLISTED_ACCOUNT rule when source account is blacklisted")
    void evaluate_BlacklistedSourceAccount_RuleTriggered() {
        FraudRule rule = buildRule(FraudRuleType.BLACKLISTED_ACCOUNT, 0.0, 80, "ACC-001,ACC-999");
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(fraudResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FraudEvaluationResult result = fraudDetectionEngineService.evaluate(payment, "IN");

        assertThat(result.score()).isEqualTo(80);
        assertThat(result.results().get(0).isTriggered()).isTrue();
    }

    @Test
    @DisplayName("Should NOT trigger BLACKLISTED_ACCOUNT rule when accounts are not blacklisted")
    void evaluate_NotBlacklistedAccount_RuleNotTriggered() {
        FraudRule rule = buildRule(FraudRuleType.BLACKLISTED_ACCOUNT, 0.0, 80, "BAD-001,BAD-002");
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(fraudResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FraudEvaluationResult result = fraudDetectionEngineService.evaluate(payment, "IN");

        assertThat(result.score()).isZero();
    }

    @Test
    @DisplayName("Should trigger VELOCITY_CHECK when count meets threshold")
    void evaluate_VelocityCheck_RuleTriggered() {
        FraudRule rule = buildRule(FraudRuleType.VELOCITY_CHECK, 3.0, 30, "Velocity check");
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(fraudResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.countBySourceAccountAndCreatedAtAfter(eq("ACC-001"), any()))
                .thenReturn(5L);

        FraudEvaluationResult result = fraudDetectionEngineService.evaluate(payment, "IN");

        assertThat(result.score()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should accumulate scores from multiple triggered rules")
    void evaluate_MultipleRulesTriggered_ScoreAccumulates() {
        payment.setAmount(new BigDecimal("50000.00"));

        FraudRule largeTxn = buildRule(FraudRuleType.LARGE_TRANSACTION, 10000.0, 40, "Large txn");
        FraudRule blacklisted = buildRule(FraudRuleType.BLACKLISTED_ACCOUNT, 0.0, 80, "ACC-001");

        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(largeTxn, blacklisted));
        when(fraudResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FraudEvaluationResult result = fraudDetectionEngineService.evaluate(payment, "IN");

        assertThat(result.score()).isEqualTo(120);
        assertThat(result.results()).hasSize(2);
    }

    @Test
    @DisplayName("Should trigger REPEATED_FAILED_ATTEMPTS when threshold met")
    void evaluate_RepeatedFailedAttempts_RuleTriggered() {
        FraudRule rule = buildRule(FraudRuleType.REPEATED_FAILED_ATTEMPTS, 3.0, 50, "Failed attempts");
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(fraudResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.countBySourceAccountAndStatus("ACC-001", PaymentStatus.FAILED))
                .thenReturn(4L);

        FraudEvaluationResult result = fraudDetectionEngineService.evaluate(payment, "IN");

        assertThat(result.score()).isEqualTo(50);
    }

    private FraudRule buildRule(FraudRuleType type, double threshold, int scoreContribution, String description) {
        FraudRule rule = new FraudRule();
        rule.setName(type.name() + "_RULE");
        rule.setDescription(description);
        rule.setRuleType(type);
        rule.setThreshold(threshold);
        rule.setScoreContribution(scoreContribution);
        rule.setEnabled(true);
        return rule;
    }
}
