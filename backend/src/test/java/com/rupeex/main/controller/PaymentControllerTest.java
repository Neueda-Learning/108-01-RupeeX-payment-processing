package com.rupeex.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.dto.PaymentResponse;
import com.rupeex.main.dto.StatusUpdateRequest;
import com.rupeex.main.dto.VerificationDecisionRequest;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.exception.PaymentNotFoundException;
import com.rupeex.main.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentResponse validPaymentResponse;

    @BeforeEach
    void setUp() {
        validPaymentResponse = new PaymentResponse();
        validPaymentResponse.setPaymentId(1L);
        validPaymentResponse.setPaymentReference("PAY-123");
        validPaymentResponse.setAmount(new BigDecimal("1000.00"));
        validPaymentResponse.setCurrency("USD");
        validPaymentResponse.setSourceAccount("ACC-001");
        validPaymentResponse.setDestinationAccount("ACC-002");
        validPaymentResponse.setStatus(PaymentStatus.COMPLETED);
        validPaymentResponse.setVerificationRequired(false);
        validPaymentResponse.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create payment and return 201 CREATED")
    void createPayment_ValidRequest_Returns201Created() throws Exception {
        PaymentRequest request = createValidPaymentRequest();
        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(validPaymentResponse);

        mockMvc.perform(post("/legacy/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId", is(1)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(paymentService, times(1)).createPayment(any(PaymentRequest.class));
    }

    @Test
    @DisplayName("Should retrieve payment by ID and return 200 OK")
    void getPaymentById_ValidId_Returns200Ok() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(validPaymentResponse);

        mockMvc.perform(get("/legacy/payments/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(1)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(paymentService, times(1)).getPaymentById(1L);
    }

    @Test
    @DisplayName("Should return 404 NOT FOUND for non-existent payment")
    void getPaymentById_InvalidId_Returns404NotFound() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new PaymentNotFoundException("Payment not found"));

        mockMvc.perform(get("/legacy/payments/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update payment status and return 204 NO CONTENT")
    void updatePaymentStatus_ValidRequest_Returns204NoContent() throws Exception {
        StatusUpdateRequest statusRequest = new StatusUpdateRequest();
        statusRequest.setStatus(PaymentStatus.DECLINED);

        doNothing().when(paymentService).updatePaymentStatus(1L, PaymentStatus.DECLINED);

        mockMvc.perform(patch("/legacy/payments/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isNoContent());

        verify(paymentService, times(1)).updatePaymentStatus(1L, PaymentStatus.DECLINED);
    }

    @Test
    @DisplayName("Should process verification decision and return 200 OK")
    void processVerificationDecision_ApprovedRequest_Returns200Ok() throws Exception {
        VerificationDecisionRequest decisionRequest = new VerificationDecisionRequest();
        decisionRequest.setToken("valid-token");
        decisionRequest.setApproved(true);

        when(paymentService.processVerificationDecision(eq(1L), any(VerificationDecisionRequest.class)))
                .thenReturn(validPaymentResponse);

        mockMvc.perform(post("/legacy/payments/1/verification-decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decisionRequest)))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).processVerificationDecision(eq(1L), any(VerificationDecisionRequest.class));
    }

    @Test
    @DisplayName("Should return JSON content type in response")
    void createPayment_ValidRequest_ReturnsJsonContentType() throws Exception {
        PaymentRequest request = createValidPaymentRequest();
        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(validPaymentResponse);

        mockMvc.perform(post("/legacy/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    private PaymentRequest createValidPaymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("USD");
        request.setSourceAccount("ACC-001");
        request.setDestinationAccount("ACC-002");
        request.setIdempotencyKey(UUID.randomUUID().toString());
        return request;
    }
}

