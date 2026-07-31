package com.rupeex.main.service.impl;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.entity.CustomerTrustProfile;
import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.TrustCategory;
import com.rupeex.main.repository.CustomerTrustProfileRepository;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.service.model.TrustAssessmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceImplTest {

    @Mock
    private CustomerTrustProfileRepository trustProfileRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private TrustScoreServiceImpl trustScoreService;

    @BeforeEach
    void setUp() {
        trustScoreService = new TrustScoreServiceImpl(
                trustProfileRepository,
                paymentRepository
        );
    }

    @Test
    void assessTrust_whenOnlyOneFactorTriggered_usesOnlyThatFactorScore() {
        CustomerTrustProfile profile = profile(0.60, 0.40, 0.90);
        PaymentRequest request = paymentRequest(new BigDecimal("1200.00"), "USD");

        Payment previousPayment = new Payment();
        previousPayment.setCurrency("INR");

        when(trustProfileRepository.findByCustomerId("SRC-1"))
                .thenReturn(Optional.of(profile));
        when(paymentRepository.findTopBySourceAccountOrderByCreatedAtDesc("SRC-1"))
                .thenReturn(Optional.of(previousPayment));
        when(paymentRepository.findAverageAmountBySourceAccount("SRC-1"))
                .thenReturn(null);
        when(paymentRepository.countBySourceAccountAndCreatedAtAfter(any(), any(LocalDateTime.class)))
                .thenReturn(0L);

        TrustAssessmentResult result = trustScoreService.assessTrust("SRC-1", request);

        assertEquals(0.60, result.getTrustScore());
        assertTrue(result.isTriggered(TrustCategory.CURRENCY_CHANGE));
        assertEquals(1, result.getTriggeredCategories().size());
    }

    @Test
    void assessTrust_whenMultipleFactorsTriggered_usesMinimumTriggeredScore() {
        CustomerTrustProfile profile = profile(0.85, 0.45, 0.70);
        PaymentRequest request = paymentRequest(new BigDecimal("5000.00"), "USD");

        Payment previousPayment = new Payment();
        previousPayment.setCurrency("INR");

        when(trustProfileRepository.findByCustomerId("SRC-1"))
                .thenReturn(Optional.of(profile));
        when(paymentRepository.findTopBySourceAccountOrderByCreatedAtDesc("SRC-1"))
                .thenReturn(Optional.of(previousPayment));
        when(paymentRepository.findAverageAmountBySourceAccount("SRC-1"))
                .thenReturn(1000.0);
        when(paymentRepository.countBySourceAccountAndCreatedAtAfter(any(), any(LocalDateTime.class)))
                .thenReturn(3L);

        TrustAssessmentResult result = trustScoreService.assessTrust("SRC-1", request);

        assertEquals(0.45, result.getTrustScore());
        assertTrue(result.isTriggered(TrustCategory.CURRENCY_CHANGE));
        assertTrue(result.isTriggered(TrustCategory.LARGE_PAYMENT));
        assertTrue(result.isTriggered(TrustCategory.RAPID_PAYMENTS));
    }

    @Test
    void assessTrust_whenNoFactorsTriggered_returnsNoTriggerAndFullTrust() {
        CustomerTrustProfile profile = profile(0.25, 0.40, 0.55);
        PaymentRequest request = paymentRequest(new BigDecimal("500.00"), "INR");

        Payment previousPayment = new Payment();
        previousPayment.setCurrency("INR");

        when(trustProfileRepository.findByCustomerId("SRC-1"))
                .thenReturn(Optional.of(profile));
        when(paymentRepository.findTopBySourceAccountOrderByCreatedAtDesc("SRC-1"))
                .thenReturn(Optional.of(previousPayment));
        when(paymentRepository.findAverageAmountBySourceAccount("SRC-1"))
                .thenReturn(1000.0);
        when(paymentRepository.countBySourceAccountAndCreatedAtAfter(any(), any(LocalDateTime.class)))
                .thenReturn(0L);

        TrustAssessmentResult result = trustScoreService.assessTrust("SRC-1", request);

        assertFalse(result.hasTriggers());
        assertEquals(1.0, result.getTrustScore());
    }

    @Test
    void applyVerificationOutcome_updatesOnlyTriggeredCategories() {
        CustomerTrustProfile profile = profile(0.50, 0.50, 0.50);

        when(trustProfileRepository.findByCustomerId("SRC-1"))
                .thenReturn(Optional.of(profile));
        when(trustProfileRepository.save(any(CustomerTrustProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TrustAssessmentResult assessmentResult = new TrustAssessmentResult(
                0.50,
                java.util.EnumSet.of(TrustCategory.LARGE_PAYMENT)
        );

        trustScoreService.applyVerificationOutcome("SRC-1", assessmentResult, true);

        assertEquals(0.50, profile.getCurrencyChangeScore());
        assertEquals(0.625, profile.getLargePaymentScore());
        assertEquals(0.50, profile.getRapidPaymentsScore());
    }

    private CustomerTrustProfile profile(
            double currencyScore,
            double largePaymentScore,
            double rapidPaymentsScore
    ) {
        CustomerTrustProfile profile = new CustomerTrustProfile();
        profile.setCustomerId("SRC-1");
        profile.setCurrencyChangeScore(currencyScore);
        profile.setLargePaymentScore(largePaymentScore);
        profile.setRapidPaymentsScore(rapidPaymentsScore);
        return profile;
    }

    private PaymentRequest paymentRequest(BigDecimal amount, String currency) {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);
        request.setCurrency(currency);
        request.setSourceAccount("SRC-1");
        request.setDestinationAccount("DST-1");
        request.setIdempotencyKey("idem-1");
        return request;
    }
}

