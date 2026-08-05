package com.rupeex.main.controller;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.dto.PaymentResponse;
import com.rupeex.main.dto.StatusUpdateRequest;
import com.rupeex.main.dto.VerificationDecisionRequest;
import com.rupeex.main.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/legacy/payments")
@Tag(name = "Payments", description = "Payment creation, retrieval, and status management")
public class PaymentController {


    private final PaymentService paymentService;


    // Constructor Injection
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }



    // Create Payment
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create payment", description = "Create a new payment transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payment request"),
            @ApiResponse(responseCode = "422", description = "Payment validation failed")
    })
    public PaymentResponse createPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment request details",
                    required = true)
            @Valid @RequestBody PaymentRequest paymentRequest) {

        return paymentService.createPayment(paymentRequest);
    }



    // Get payment by ID
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details using the payment ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found and returned"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentResponse getPaymentById(
            @Parameter(description = "Payment ID", example = "12345", required = true)
            @PathVariable Long paymentId) {

        return paymentService.getPaymentById(paymentId);
    }



    // Update payment status
    @PatchMapping("/{paymentId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update payment status", description = "Update the status of an existing payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "400", description = "Invalid status update request")
    })
    public void updatePaymentStatus(
            @Parameter(description = "Payment ID", example = "12345", required = true)
            @PathVariable Long paymentId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Status update request",
                    required = true)
            @Valid @RequestBody StatusUpdateRequest request) {

        paymentService.updatePaymentStatus(
                paymentId,
                request.getStatus()
        );
    }



    @PostMapping("/{paymentId}/verification-decision")
    @Operation(summary = "Process verification decision", description = "Process fraud verification decision for a payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification decision processed successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "400", description = "Invalid verification decision request")
    })
    public PaymentResponse processVerificationDecision(
            @Parameter(description = "Payment ID", example = "12345", required = true)
            @PathVariable Long paymentId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Verification decision details",
                    required = true)
            @Valid @RequestBody VerificationDecisionRequest request) {

        return paymentService.processVerificationDecision(
                paymentId,
                request
        );
    }

}