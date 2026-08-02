package com.rupeex.main.service.impl;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.entity.CustomerTrustProfile;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.TrustCategory;
import com.rupeex.main.repository.CustomerTrustProfileRepository;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.service.TrustScoreService;
import com.rupeex.main.service.model.TrustAssessmentResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class TrustScoreServiceImpl implements TrustScoreService {

    private static final double DEFAULT_SCORE = 0.5;
    private static final double APPROVE_MULTIPLIER = 1.25;
    private static final double DECLINE_MULTIPLIER = 0.75;
    private static final BigDecimal LARGE_PAYMENT_MULTIPLIER = new BigDecimal("2.00");
    private static final int RAPID_PAYMENTS_THRESHOLD = 3;
    private static final int RAPID_PAYMENTS_WINDOW_MINUTES = 10;

    private final CustomerTrustProfileRepository trustProfileRepository;
    private final PaymentRepository paymentRepository;

    public TrustScoreServiceImpl(
            CustomerTrustProfileRepository trustProfileRepository,
            PaymentRepository paymentRepository
    ) {
        this.trustProfileRepository = trustProfileRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public TrustAssessmentResult assessTrust(
            String customerId,
            PaymentRequest request
    ) {
        CustomerTrustProfile profile = getOrCreateProfile(customerId);
        List<Payment> customerPayments = getCustomerPayments(customerId);

        Set<TrustCategory> triggeredCategories = EnumSet.noneOf(TrustCategory.class);
        List<Double> triggeredScores = new ArrayList<>();

        if (isCurrencyChangeTriggered(customerPayments, request.getCurrency())) {
            triggeredCategories.add(TrustCategory.CURRENCY_CHANGE);
            triggeredScores.add(profile.getCurrencyChangeScore());
        }

        if (isLargePaymentTriggered(customerPayments, request.getAmount())) {
            triggeredCategories.add(TrustCategory.LARGE_PAYMENT);
            triggeredScores.add(profile.getLargePaymentScore());
        }

        if (isRapidPaymentsTriggered(customerPayments)) {
            triggeredCategories.add(TrustCategory.RAPID_PAYMENTS);
            triggeredScores.add(profile.getRapidPaymentsScore());
        }

        double trustScore = 1.0;

        if (triggeredScores.size() == 1) {
            trustScore = triggeredScores.getFirst();
        } else if (!triggeredScores.isEmpty()) {
            trustScore = triggeredScores.stream()
                    .min(Double::compareTo)
                    .orElse(1.0);
        }

        return new TrustAssessmentResult(
                round(trustScore),
                triggeredCategories
        );
    }

    @Override
    @Transactional
    public void applyVerificationOutcome(
            String customerId,
            TrustAssessmentResult assessmentResult,
            boolean approved
    ) {
        CustomerTrustProfile profile = getOrCreateProfile(customerId);

        if (assessmentResult.isTriggered(TrustCategory.CURRENCY_CHANGE)) {
            profile.setCurrencyChangeScore(
                    adjustScore(profile.getCurrencyChangeScore(), approved)
            );
        }

        if (assessmentResult.isTriggered(TrustCategory.LARGE_PAYMENT)) {
            profile.setLargePaymentScore(
                    adjustScore(profile.getLargePaymentScore(), approved)
            );
        }

        if (assessmentResult.isTriggered(TrustCategory.RAPID_PAYMENTS)) {
            profile.setRapidPaymentsScore(
                    adjustScore(profile.getRapidPaymentsScore(), approved)
            );
        }

        trustProfileRepository.save(profile);
    }

    private CustomerTrustProfile getOrCreateProfile(String customerId) {
        return trustProfileRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    CustomerTrustProfile profile = new CustomerTrustProfile();
                    profile.setCustomerId(customerId);
                    profile.setCurrencyChangeScore(DEFAULT_SCORE);
                    profile.setLargePaymentScore(DEFAULT_SCORE);
                    profile.setRapidPaymentsScore(DEFAULT_SCORE);
                    return trustProfileRepository.save(profile);
                });
    }

    private List<Payment> getCustomerPayments(String customerId) {
        return paymentRepository.findAll().stream()
                .filter(payment -> customerId.equals(payment.getSourceAccount()))
                .toList();
    }

    private boolean isCurrencyChangeTriggered(List<Payment> customerPayments, String currency) {
        return customerPayments.stream()
                .max(Comparator.comparing(Payment::getCreatedAt))
                .map(payment -> !payment.getCurrency().equalsIgnoreCase(currency))
                .orElse(false);
    }

    private boolean isLargePaymentTriggered(List<Payment> customerPayments, BigDecimal amount) {
        if (customerPayments.isEmpty()) {
            return false;
        }

        double averageAmount = customerPayments.stream()
                .map(Payment::getAmount)
                .filter(paymentAmount -> paymentAmount != null)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        if (averageAmount <= 0) {
            return false;
        }

        BigDecimal threshold = BigDecimal.valueOf(averageAmount)
                .multiply(LARGE_PAYMENT_MULTIPLIER);

        return amount.compareTo(threshold) > 0;
    }

    private boolean isRapidPaymentsTriggered(List<Payment> customerPayments) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(RAPID_PAYMENTS_WINDOW_MINUTES);
        long recentPayments = customerPayments.stream()
                .map(Payment::getCreatedAt)
                .filter(createdAt -> createdAt != null && createdAt.isAfter(threshold))
                .count();
        return recentPayments >= RAPID_PAYMENTS_THRESHOLD;
    }

    private double adjustScore(double currentScore, boolean approved) {
        double adjustedScore = approved
                ? currentScore * APPROVE_MULTIPLIER
                : currentScore * DECLINE_MULTIPLIER;

        if (adjustedScore > 1.0) {
            adjustedScore = 1.0;
        }

        if (adjustedScore < 0.0) {
            adjustedScore = 0.0;
        }

        return round(adjustedScore);
    }

    private double round(double value) {
        return new BigDecimal(Double.toString(value))
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

