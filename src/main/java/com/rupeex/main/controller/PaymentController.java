package com.rupeex.main.controller;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.dto.PaymentResponse;
import com.rupeex.main.dto.StatusUpdateRequest;
import com.rupeex.main.dto.VerificationDecisionRequest;
import com.rupeex.main.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/payments")
public class PaymentController {


    private final PaymentService paymentService;


    // Constructor Injection
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }



    // Create Payment
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(
            @Valid @RequestBody PaymentRequest paymentRequest) {

        return paymentService.createPayment(paymentRequest);
    }



    // Get payment by ID
    @GetMapping("/{paymentId}")
    public PaymentResponse getPaymentById(
            @PathVariable Long paymentId) {

        return paymentService.getPaymentById(paymentId);
    }



    // Update payment status
    @PatchMapping("/{paymentId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePaymentStatus(
            @PathVariable Long paymentId,
            @Valid @RequestBody StatusUpdateRequest request) {

        paymentService.updatePaymentStatus(
                paymentId,
                request.getStatus()
        );
    }



    @PostMapping("/{paymentId}/verification-decision")
    public PaymentResponse processVerificationDecision(
            @PathVariable Long paymentId,
            @Valid @RequestBody VerificationDecisionRequest request) {

        return paymentService.processVerificationDecision(
                paymentId,
                request
        );
    }

}