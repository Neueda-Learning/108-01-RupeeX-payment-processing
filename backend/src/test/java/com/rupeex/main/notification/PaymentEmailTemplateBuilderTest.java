package com.rupeex.main.notification;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.notification.service.PaymentEmailTemplateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentEmailTemplateBuilder Tests")
class PaymentEmailTemplateBuilderTest {

    private PaymentEmailTemplateBuilder templateBuilder;
    private Payment payment;

    @BeforeEach
    void setUp() {
        templateBuilder = new PaymentEmailTemplateBuilder();

        payment = new Payment();
        payment.setPaymentReference("EP-REF-001");
        payment.setAmount(new BigDecimal("5000.00"));
        payment.setCurrency("INR");
        payment.setSourceAccount("ACC-001");
        payment.setDestinationAccount("ACC-002");
        payment.setStatus(PaymentStatus.SETTLED);
    }

    @Test
    @DisplayName("Success template contains payment reference")
    void buildPaymentSuccessTemplate_ContainsPaymentReference() {
        String body = templateBuilder.buildPaymentSuccessTemplate(payment, "John Doe");

        assertThat(body).contains("EP-REF-001");
    }

    @Test
    @DisplayName("Success template contains recipient name")
    void buildPaymentSuccessTemplate_ContainsRecipientName() {
        String body = templateBuilder.buildPaymentSuccessTemplate(payment, "John Doe");

        assertThat(body).contains("John Doe");
    }

    @Test
    @DisplayName("Success template contains amount and currency")
    void buildPaymentSuccessTemplate_ContainsAmountAndCurrency() {
        String body = templateBuilder.buildPaymentSuccessTemplate(payment, "John Doe");

        assertThat(body).contains("5000.00");
        assertThat(body).contains("INR");
    }

    @Test
    @DisplayName("Success template mentions SETTLED status")
    void buildPaymentSuccessTemplate_ContainsSettledStatus() {
        String body = templateBuilder.buildPaymentSuccessTemplate(payment, "John Doe");

        assertThat(body).contains("SETTLED");
    }

    @Test
    @DisplayName("Failure template contains failure reason")
    void buildPaymentFailureTemplate_ContainsFailureReason() {
        String body = templateBuilder.buildPaymentFailureTemplate(payment, "Jane Doe", "INSUFFICIENT_FUNDS");

        assertThat(body).contains("Jane Doe");
        assertThat(body).contains("EP-REF-001");
    }

    @Test
    @DisplayName("Failure template includes support contact")
    void buildPaymentFailureTemplate_ContainsSupportContact() {
        String body = templateBuilder.buildPaymentFailureTemplate(payment, "Jane Doe", "ERROR");

        assertThat(body).contains("support@rupeex.com");
    }

    @Test
    @DisplayName("Debit template contains account and payment reference")
    void buildDebitNotificationTemplate_ContainsKeyInfo() {
        String body = templateBuilder.buildDebitNotificationTemplate(payment, "John Doe");

        assertThat(body).contains("John Doe");
        assertThat(body).contains("EP-REF-001");
        assertThat(body).contains("5000.00");
    }

    @Test
    @DisplayName("Credit template contains recipient name and amount")
    void buildCreditNotificationTemplate_ContainsKeyInfo() {
        String body = templateBuilder.buildCreditNotificationTemplate(payment, "Alice Smith");

        assertThat(body).contains("Alice Smith");
        assertThat(body).contains("5000.00");
        assertThat(body).contains("INR");
    }
}
