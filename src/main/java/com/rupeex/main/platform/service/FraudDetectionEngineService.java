package com.rupeex.main.platform.service;

import com.rupeex.main.entity.FraudResult;
import com.rupeex.main.entity.FraudRule;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.FraudRuleType;
import com.rupeex.main.repository.FraudResultRepository;
import com.rupeex.main.repository.FraudRuleRepository;
import com.rupeex.main.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FraudDetectionEngineService {

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudResultRepository fraudResultRepository;
    private final PaymentRepository paymentRepository;

    public FraudDetectionEngineService(FraudRuleRepository fraudRuleRepository,
                                       FraudResultRepository fraudResultRepository,
                                       PaymentRepository paymentRepository) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudResultRepository = fraudResultRepository;
        this.paymentRepository = paymentRepository;
    }

    public FraudEvaluationResult evaluate(Payment payment, String originCountry) {
        List<FraudRule> activeRules = fraudRuleRepository.findByEnabledTrue();
        List<FraudResult> outputs = new ArrayList<>();
        int totalScore = 0;
        StringBuilder explanation = new StringBuilder();

        for (FraudRule rule : activeRules) {
            boolean triggered = isTriggered(rule, payment, originCountry);
            FraudResult result = new FraudResult();
            result.setPaymentId(payment.getId());
            result.setRuleId(rule.getId());
            result.setRuleName(rule.getName());
            result.setTriggered(triggered);
            result.setScoreContribution(triggered ? rule.getScoreContribution() : 0);
            result.setReason(triggered ? "Rule matched" : "Rule not matched");
            outputs.add(fraudResultRepository.save(result));

            if (triggered) {
                totalScore += rule.getScoreContribution();
                explanation.append(rule.getName())
                        .append(" +")
                        .append(rule.getScoreContribution())
                        .append("; ");
            }
        }

        return new FraudEvaluationResult(totalScore, outputs, explanation.toString().trim());
    }

    private boolean isTriggered(FraudRule rule, Payment payment, String originCountry) {
        return switch (rule.getRuleType()) {
            case LARGE_TRANSACTION -> payment.getAmount().doubleValue() >= rule.getThreshold();
            case NIGHT_TRANSACTION -> {
                int hour = LocalDateTime.now().getHour();
                yield hour < 6 || hour >= 23;
            }
            case VELOCITY_CHECK, SUSPICIOUS_FREQUENCY ->
                    paymentRepository.countBySourceAccountAndCreatedAtAfter(
                            payment.getSourceAccount(),
                            LocalDateTime.now().minusMinutes((long) rule.getThreshold())) >= 5;
            case REPEATED_FAILED_ATTEMPTS ->
                    paymentRepository.findByStatus(com.rupeex.main.enums.PaymentStatus.FAILED).stream()
                            .filter(p -> p.getSourceAccount().equals(payment.getSourceAccount()))
                            .count() >= (long) rule.getThreshold();
            case BLACKLISTED_ACCOUNT -> payment.getSourceAccount().startsWith("BLK");
            case HIGH_RISK_COUNTRY -> "IR".equalsIgnoreCase(originCountry) || "KP".equalsIgnoreCase(originCountry);
            case NEW_ACCOUNT -> paymentRepository.findTopBySourceAccountOrderByCreatedAtDesc(payment.getSourceAccount()).isEmpty();
        };
    }
}
