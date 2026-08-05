package com.rupeex.main.controller;

import com.rupeex.main.platform.dto.PaymentPlatformRequest;
import com.rupeex.main.platform.dto.PaymentPlatformResponse;
import com.rupeex.main.platform.service.PaymentOrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentPlatformController.class)
@DisplayName("PaymentPlatformController Tests")
class PaymentPlatformControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentOrchestrationService orchestrationService;

    @Test
    @DisplayName("Should create payment and return 201")
    void createPayment_ValidRequest_Returns201() throws Exception {
        PaymentPlatformResponse response = new PaymentPlatformResponse();
        when(orchestrationService.createPayment(any(PaymentPlatformRequest.class))).thenReturn(response);

        mockMvc.perform(post("/payments")
                .contentType("application/json")
                .content("{\"sourceAccount\":\"ACC-001\",\"destinationAccount\":\"ACC-002\",\"amount\":1000.00,\"currency\":\"USD\",\"idempotencyKey\":\"key-123\",\"originCountry\":\"US\",\"destinationCountry\":\"US\"}"))
                .andExpect(status().isCreated());

        verify(orchestrationService, times(1)).createPayment(any());
    }

    @Test
    @DisplayName("Should get payments with pagination")
    void getPayments_WithPagination_Success() throws Exception {
        when(orchestrationService.getPayments(any(Pageable.class))).thenReturn(new PageImpl<>(new ArrayList<>()));

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk());

        verify(orchestrationService, times(1)).getPayments(any());
    }

    @Test
    @DisplayName("Should get payment by ID")
    void getPayment_ValidId_Success() throws Exception {
        PaymentPlatformResponse response = new PaymentPlatformResponse();
        when(orchestrationService.getPayment(1L)).thenReturn(response);

        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isOk());

        verify(orchestrationService, times(1)).getPayment(1L);
    }

    @Test
    @DisplayName("Should retry payment returns 200")
    void retry_ValidId_Returns200() throws Exception {
        PaymentPlatformResponse response = new PaymentPlatformResponse();
        when(orchestrationService.retryPayment(1L)).thenReturn(response);

        mockMvc.perform(post("/payments/1/retry"))
                .andExpect(status().isOk());

        verify(orchestrationService, times(1)).retryPayment(1L);
    }

    @Test
    @DisplayName("Should cancel payment returns 200")
    void cancel_ValidId_Returns200() throws Exception {
        PaymentPlatformResponse response = new PaymentPlatformResponse();
        when(orchestrationService.cancelPayment(1L)).thenReturn(response);

        mockMvc.perform(post("/payments/1/cancel"))
                .andExpect(status().isOk());

        verify(orchestrationService, times(1)).cancelPayment(1L);
    }

    @Test
    @DisplayName("Should get payment history")
    void history_ValidId_Success() throws Exception {
        when(orchestrationService.history(1L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/payments/1/history"))
                .andExpect(status().isOk());

        verify(orchestrationService, times(1)).history(1L);
    }
}

