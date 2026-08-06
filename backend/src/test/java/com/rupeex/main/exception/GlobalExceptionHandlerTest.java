package com.rupeex.main.exception;

import com.rupeex.main.controller.PaymentController;
import com.rupeex.main.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    @DisplayName("PaymentNotFoundException returns 404 with problem detail")
    void handlePaymentNotFound_Returns404() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new PaymentNotFoundException("Payment not found: 999"));

        mockMvc.perform(get("/legacy/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Payment Not Found"))
                .andExpect(jsonPath("$.detail").value("Payment not found: 999"));
    }

    @Test
    @DisplayName("DuplicatePaymentException returns 409 with problem detail")
    void handleDuplicatePayment_Returns409() throws Exception {
        when(paymentService.createPayment(any()))
                .thenThrow(new DuplicatePaymentException("Duplicate payment"));

        mockMvc.perform(post("/legacy/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"currency\":\"INR\",\"sourceAccount\":\"ACC-001\"," +
                                "\"destinationAccount\":\"ACC-002\",\"idempotencyKey\":\"key-001\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Payment"));
    }

    @Test
    @DisplayName("Bean validation failure returns 400 with field errors")
    void handleValidationError_Returns400WithErrors() throws Exception {
        // amount is missing / null — should fail @NotNull validation
        mockMvc.perform(post("/legacy/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"INR\",\"sourceAccount\":\"ACC-001\"," +
                                "\"destinationAccount\":\"ACC-002\",\"idempotencyKey\":\"key-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"));
    }
}
