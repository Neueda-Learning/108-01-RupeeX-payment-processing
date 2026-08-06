package com.rupeex.main.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rupeex.main.client.ExchangeApiClient;
import com.rupeex.main.dto.ExchangeRequest;
import com.rupeex.main.dto.ExchangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateServiceImpl Tests")
class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeApiClient exchangeApiClient;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exchangeRateService = new ExchangeRateServiceImpl(exchangeApiClient, objectMapper);
    }

    @Test
    @DisplayName("Should convert USD to INR successfully")
    void convert_UsdToInr_ReturnsCorrectConversion() {
        String apiResponse = "{\"conversion_rates\":{\"INR\":83.50}}";
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setFromCurrency("USD");
        request.setToCurrency("INR");

        when(exchangeApiClient.getRates("USD")).thenReturn(apiResponse);

        ExchangeResponse response = exchangeRateService.convert(request);

        assertThat(response.getFromCurrency()).isEqualTo("USD");
        assertThat(response.getToCurrency()).isEqualTo("INR");
        assertThat(response.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getExchangeRate()).isEqualByComparingTo(new BigDecimal("83.50"));
        assertThat(response.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("8350.00"));
    }

    @Test
    @DisplayName("Should convert EUR to USD successfully")
    void convert_EurToUsd_ReturnsCorrectConversion() {
        String apiResponse = "{\"conversion_rates\":{\"USD\":1.09}}";
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setFromCurrency("EUR");
        request.setToCurrency("USD");

        when(exchangeApiClient.getRates("EUR")).thenReturn(apiResponse);

        ExchangeResponse response = exchangeRateService.convert(request);

        assertThat(response.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("54.50"));
    }

    @Test
    @DisplayName("Should throw RuntimeException when API returns no conversion_rates")
    void convert_MissingConversionRates_ThrowsRuntimeException() {
        String apiResponse = "{\"result\":\"error\"}";
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setFromCurrency("USD");
        request.setToCurrency("INR");

        when(exchangeApiClient.getRates("USD")).thenReturn(apiResponse);

        assertThatThrownBy(() -> exchangeRateService.convert(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Currency conversion failed");
    }

    @Test
    @DisplayName("Should throw RuntimeException when target currency not in rates")
    void convert_UnknownTargetCurrency_ThrowsRuntimeException() {
        String apiResponse = "{\"conversion_rates\":{\"EUR\":0.92}}";
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setFromCurrency("USD");
        request.setToCurrency("XYZ");

        when(exchangeApiClient.getRates("USD")).thenReturn(apiResponse);

        assertThatThrownBy(() -> exchangeRateService.convert(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Currency conversion failed");
    }

    @Test
    @DisplayName("Should round converted amount to 2 decimal places")
    void convert_RoundingApplied_ResultHasTwoDecimalPlaces() {
        String apiResponse = "{\"conversion_rates\":{\"INR\":83.333333}}";
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setFromCurrency("USD");
        request.setToCurrency("INR");

        when(exchangeApiClient.getRates("USD")).thenReturn(apiResponse);

        ExchangeResponse response = exchangeRateService.convert(request);

        assertThat(response.getConvertedAmount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw RuntimeException when API call fails")
    void convert_ApiCallFails_ThrowsRuntimeException() {
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setFromCurrency("USD");
        request.setToCurrency("INR");

        when(exchangeApiClient.getRates("USD")).thenThrow(new RuntimeException("API unavailable"));

        assertThatThrownBy(() -> exchangeRateService.convert(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Currency conversion failed");
    }
}
