package com.rupeex.main.validation;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.exception.InvalidPaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentValidator Tests")
class PaymentValidatorTest {

    private PaymentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PaymentValidator();
    }

    private PaymentRequest buildRequest(BigDecimal amount, String currency, String source, String destination) {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);
        request.setCurrency(currency);
        request.setSourceAccount(source);
        request.setDestinationAccount(destination);
        request.setIdempotencyKey("key-001");
        return request;
    }

    @Test
    @DisplayName("Should pass validation for a valid request")
    void validate_ValidRequest_NoException() {
        PaymentRequest request = buildRequest(new BigDecimal("100.00"), "INR", "ACC-001", "ACC-002");

        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Should throw when amount is null")
    void validate_NullAmount_ThrowsInvalidPaymentException() {
        PaymentRequest request = buildRequest(null, "INR", "ACC-001", "ACC-002");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw when amount is zero")
    void validate_ZeroAmount_ThrowsInvalidPaymentException() {
        PaymentRequest request = buildRequest(BigDecimal.ZERO, "INR", "ACC-001", "ACC-002");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw when amount is negative")
    void validate_NegativeAmount_ThrowsInvalidPaymentException() {
        PaymentRequest request = buildRequest(new BigDecimal("-50.00"), "INR", "ACC-001", "ACC-002");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw when currency is unsupported")
    void validate_UnsupportedCurrency_ThrowsInvalidPaymentException() {
        PaymentRequest request = buildRequest(new BigDecimal("100.00"), "JPY", "ACC-001", "ACC-002");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Currency not supported");
    }

    @Test
    @DisplayName("Should pass for all supported currencies")
    void validate_AllSupportedCurrencies_NoException() {
        for (String currency : new String[]{"INR", "USD", "EUR", "GBP"}) {
            PaymentRequest request = buildRequest(new BigDecimal("100.00"), currency, "ACC-001", "ACC-002");
            assertThatNoException().isThrownBy(() -> validator.validate(request));
        }
    }

    @Test
    @DisplayName("Should throw when source and destination accounts are the same")
    void validate_SameSourceAndDestination_ThrowsInvalidPaymentException() {
        PaymentRequest request = buildRequest(new BigDecimal("100.00"), "INR", "ACC-001", "ACC-001");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Source and destination account cannot be same");
    }
}
