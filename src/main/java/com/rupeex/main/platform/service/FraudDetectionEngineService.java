package com.rupeex.main.platform.service;

import com.rupeex.main.entity.FraudResult;
import com.rupeex.main.entity.FraudRule;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.FraudRuleType;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.repository.AccountsRepository;
import com.rupeex.main.repository.FraudResultRepository;
import com.rupeex.main.repository.FraudRuleRepository;
import com.rupeex.main.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FraudDetectionEngineService {

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudResultRepository fraudResultRepository;
    private final PaymentRepository paymentRepository;
    private final AccountsRepository accountsRepository;

    public FraudDetectionEngineService(FraudRuleRepository fraudRuleRepository,
                                       FraudResultRepository fraudResultRepository,
                                       PaymentRepository paymentRepository,
                                       AccountsRepository accountsRepository) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudResultRepository = fraudResultRepository;
        this.paymentRepository = paymentRepository;
        this.accountsRepository = accountsRepository;
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
            result.setReason(triggered ? buildTriggerReason(rule, payment, originCountry) : "No match");
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

            case LARGE_TRANSACTION ->
                    payment.getAmount().doubleValue() > rule.getThreshold();

            case NIGHT_TRANSACTION -> {
                int hour = LocalDateTime.now().getHour();
                yield hour < 6 || hour >= 22;
            }

            // VELOCITY_CHECK: threshold = max allowed transactions in a 10-minute window
            case VELOCITY_CHECK, SUSPICIOUS_FREQUENCY -> {
                long count = paymentRepository.countBySourceAccountAndCreatedAtAfter(
                        payment.getSourceAccount(),
                        LocalDateTime.now().minusMinutes(10));
                yield count >= (long) rule.getThreshold();
            }

            // REPEATED_FAILED_ATTEMPTS: threshold = how many failures to trigger
            case REPEATED_FAILED_ATTEMPTS -> {
                long failCount = paymentRepository.countBySourceAccountAndStatus(
                        payment.getSourceAccount(), PaymentStatus.FAILED);
                yield failCount >= (long) rule.getThreshold();
            }

            // BLACKLISTED_ACCOUNT: rule description holds comma-separated account numbers
            case BLACKLISTED_ACCOUNT -> {
                Set<String> blocked = parseListFromDescription(rule.getDescription());
                yield blocked.contains(payment.getSourceAccount())
                        || blocked.contains(payment.getDestinationAccount());
            }

            // HIGH_RISK_COUNTRY: rule description holds [Countries:XX,YY,ZZ] encoded by the UI
            case HIGH_RISK_COUNTRY -> {
                Set<String> risky = parseCountriesFromDescription(rule.getDescription());
                String origin = originCountry != null ? originCountry.toUpperCase() : "";
                String destCountry = getAccountCountry(payment.getDestinationAccount());
                yield risky.contains(origin) || risky.contains(destCountry);
            }

            // NEW_ACCOUNT: threshold = account age limit in days
            case NEW_ACCOUNT -> {
                int ageLimitDays = (int) rule.getThreshold();
                if (ageLimitDays <= 0) ageLimitDays = 30;
                yield isNewAccount(payment.getSourceAccount(), ageLimitDays);
            }
        };
    }

    private String getAccountCountry(String accountNumber) {
        return accountsRepository.findByAccountNumber(accountNumber)
                .map(com.rupeex.main.entity.Account::getCountryCode)
                .map(String::toUpperCase)
                .orElse("");
    }

    private boolean isNewAccount(String accountNumber, int ageLimitDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ageLimitDays);
        return accountsRepository.findByAccountNumber(accountNumber)
                .map(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoff))
                .orElse(false);
    }

    /**
     * Parse [Countries:IN,US,DE,...] embedded by the frontend fraud rule form.
     */
    private Set<String> parseCountriesFromDescription(String description) {
        if (description == null) return Set.of();
        int start = description.indexOf("[Countries:");
        if (start == -1) return Set.of();
        int end = description.indexOf("]", start);
        if (end == -1) return Set.of();
        String csv = description.substring(start + "[Countries:".length(), end);
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Parse comma-separated account numbers from rule description for BLACKLISTED_ACCOUNT.
     */
    private Set<String> parseListFromDescription(String description) {
        if (description == null || description.isBlank()) return Set.of();
        return Arrays.stream(description.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private String buildTriggerReason(FraudRule rule, Payment payment, String originCountry) {
        return switch (rule.getRuleType()) {
            case LARGE_TRANSACTION -> "Amount " + payment.getAmount() + " exceeds threshold " + rule.getThreshold();
            case NIGHT_TRANSACTION -> "Transaction at off-hours (" + LocalDateTime.now().getHour() + ":xx)";
            case VELOCITY_CHECK, SUSPICIOUS_FREQUENCY -> "High transaction frequency from " + payment.getSourceAccount();
            case REPEATED_FAILED_ATTEMPTS -> "Repeated failures from " + payment.getSourceAccount();
            case BLACKLISTED_ACCOUNT -> "Account on blocklist";
            case HIGH_RISK_COUNTRY -> "Origin/destination country flagged: " + originCountry;
            case NEW_ACCOUNT -> "Source account is recently created";
        };
    }
}
