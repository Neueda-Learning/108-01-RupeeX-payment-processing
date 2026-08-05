package com.rupeex.main.service.impl;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.exception.InvalidPaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentValidationServiceImpl Tests")
class PaymentValidationServiceImplTest {

    private PaymentValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        validationService = new PaymentValidationServiceImpl();
    }

    @Test
    @DisplayName("Should validate payment with valid request")
    void validate_ValidRequest_Success() {
        PaymentRequest request = createValidPaymentRequest();
        assertThatNoException().isThrownBy(() -> validationService.validate(request));
    }

    @Test
    @DisplayName("Should throw exception when amount is zero")
    void validate_ZeroAmount_ThrowsException() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(BigDecimal.ZERO);
        request.setCurrency("USD");
        request.setSourceAccount("ACC-001");
        request.setDestinationAccount("ACC-002");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        assertThatThrownBy(() -> validationService.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessage("Invalid amount");
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void validate_NegativeAmount_ThrowsException() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("-100.00"));
        request.setCurrency("USD");
        request.setSourceAccount("ACC-001");
        request.setDestinationAccount("ACC-002");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        assertThatThrownBy(() -> validationService.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessage("Invalid amount");
    }

    @Test
    @DisplayName("Should throw exception when source and destination accounts are same")
    void validate_SameSourceAndDestination_ThrowsException() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("USD");
        request.setSourceAccount("ACC-001");
        request.setDestinationAccount("ACC-001");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        assertThatThrownBy(() -> validationService.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessage("Same account");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-1.00", "-1000.00"})
    @DisplayName("Should throw exception for all negative amounts")
    void validate_VariousNegativeAmounts_ThrowsException(String negativeAmount) {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal(negativeAmount));
        request.setCurrency("USD");
        request.setSourceAccount("ACC-001");
        request.setDestinationAccount("ACC-002");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        assertThatThrownBy(() -> validationService.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessage("Invalid amount");
    }

    @Test
    @DisplayName("Should validate payment with minimum valid amount")
    void validate_MinimumValidAmount_Success() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("0.01"));
        request.setCurrency("USD");
        request.setSourceAccount("ACC-001");
        request.setDestinationAccount("ACC-002");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        assertThatNoException().isThrownBy(() -> validationService.validate(request));
    }

    private PaymentRequest createValidPaymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("USD");
        request.setSourceAccount("ACC-SRC-001");
        request.setDestinationAccount("ACC-DST-001");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        return request;
    }
}

