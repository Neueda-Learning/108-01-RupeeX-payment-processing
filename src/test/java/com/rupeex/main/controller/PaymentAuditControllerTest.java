package com.rupeex.main.controller;

import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.service.PaymentAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentAuditController.class)
@DisplayName("PaymentAuditController Tests")
class PaymentAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentAuditService paymentAuditService;

    @Test
    @DisplayName("Should get payment history")
    void getPaymentHistory_ValidPaymentId_Success() throws Exception {
        when(paymentAuditService.getHistory(1L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/audit/payments/1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(paymentAuditService, times(1)).getHistory(1L);
    }

    @Test
    @DisplayName("Should return empty history")
    void getPaymentHistory_NoHistory_ReturnsEmptyList() throws Exception {
        when(paymentAuditService.getHistory(999L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/audit/payments/999/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return JSON content type")
    void getPaymentHistory_CorrectContentType_Success() throws Exception {
        when(paymentAuditService.getHistory(anyLong())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/audit/payments/1/logs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}

