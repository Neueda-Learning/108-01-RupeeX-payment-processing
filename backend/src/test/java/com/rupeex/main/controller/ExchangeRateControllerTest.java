package com.rupeex.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rupeex.main.dto.ExchangeRequest;
import com.rupeex.main.dto.ExchangeResponse;
import com.rupeex.main.service.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
@DisplayName("ExchangeRateController Tests")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("Should convert currency successfully")
    void convert_ValidRequest_ReturnsConvertedAmount() throws Exception {
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setFromCurrency("USD");
        request.setToCurrency("INR");

        ExchangeResponse response = new ExchangeResponse();
        response.setOriginalAmount(new BigDecimal("100.00"));
        response.setFromCurrency("USD");
        response.setToCurrency("INR");
        response.setExchangeRate(new BigDecimal("83.50"));
        response.setConvertedAmount(new BigDecimal("8350.00"));

        when(exchangeRateService.convert(any(ExchangeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/exchange/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.toCurrency").value("INR"))
                .andExpect(jsonPath("$.originalAmount").value(100.00))
                .andExpect(jsonPath("$.convertedAmount").value(8350.00));

        verify(exchangeRateService, times(1)).convert(any(ExchangeRequest.class));
    }

    @Test
    @DisplayName("Should return JSON content type")
    void convert_ReturnsJsonContentType() throws Exception {
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setFromCurrency("EUR");
        request.setToCurrency("USD");

        ExchangeResponse response = new ExchangeResponse();
        response.setOriginalAmount(new BigDecimal("50.00"));
        response.setFromCurrency("EUR");
        response.setToCurrency("USD");
        response.setExchangeRate(new BigDecimal("1.09"));
        response.setConvertedAmount(new BigDecimal("54.50"));

        when(exchangeRateService.convert(any())).thenReturn(response);

        mockMvc.perform(post("/exchange/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should propagate service exception as 500")
    void convert_ServiceThrowsException_Returns500() throws Exception {
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setFromCurrency("USD");
        request.setToCurrency("XYZ");

        when(exchangeRateService.convert(any())).thenThrow(new RuntimeException("Currency conversion failed"));

        mockMvc.perform(post("/exchange/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle GBP to INR conversion")
    void convert_GbpToInr_ReturnsCorrectResponse() throws Exception {
        ExchangeRequest request = new ExchangeRequest();
        request.setAmount(new BigDecimal("200.00"));
        request.setFromCurrency("GBP");
        request.setToCurrency("INR");

        ExchangeResponse response = new ExchangeResponse();
        response.setOriginalAmount(new BigDecimal("200.00"));
        response.setFromCurrency("GBP");
        response.setToCurrency("INR");
        response.setExchangeRate(new BigDecimal("107.00"));
        response.setConvertedAmount(new BigDecimal("21400.00"));

        when(exchangeRateService.convert(any())).thenReturn(response);

        mockMvc.perform(post("/exchange/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRate").value(107.00))
                .andExpect(jsonPath("$.convertedAmount").value(21400.00));
    }
}
