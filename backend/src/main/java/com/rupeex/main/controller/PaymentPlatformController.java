package com.rupeex.main.controller;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.platform.dto.PaymentPlatformRequest;
import com.rupeex.main.platform.dto.PaymentPlatformResponse;
import com.rupeex.main.platform.service.PaymentOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payment Platform", description = "Advanced payment platform operations")
public class PaymentPlatformController {

    private final PaymentOrchestrationService orchestrationService;

    public PaymentPlatformController(PaymentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create payment", description = "Create a new payment using the payment platform orchestration service")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payment request")
    })
    public PaymentPlatformResponse createPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment platform request",
                    required = true)
            @Valid @RequestBody PaymentPlatformRequest request) {
        return orchestrationService.createPayment(request);
    }

    @GetMapping
    @Operation(summary = "Get paginated payments", description = "Retrieve a paginated list of all payments in the system")
    @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    public Page<Payment> getPayments(
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable) {
        return orchestrationService.getPayments(pageable);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all payments with risk scores", description = "Retrieve all payments with risk scores and fraud results included, ordered by newest first")
    @ApiResponse(responseCode = "200", description = "Payments with risk scores retrieved successfully")
    public List<PaymentPlatformResponse> getAllPaymentsWithRiskScores() {
        return orchestrationService.getAllPaymentsWithRiskScores();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Retrieve detailed information for a specific payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found and returned"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentPlatformResponse getPayment(
            @Parameter(description = "Payment ID", example = "12345", required = true)
            @PathVariable Long id) {
        return orchestrationService.getPayment(id);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry payment", description = "Retry processing a failed payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment retry initiated successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentPlatformResponse retry(
            @Parameter(description = "Payment ID", example = "12345", required = true)
            @PathVariable Long id) {
        return orchestrationService.retryPayment(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel payment", description = "Cancel a payment transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentPlatformResponse cancel(
            @Parameter(description = "Payment ID", example = "12345", required = true)
            @PathVariable Long id) {
        return orchestrationService.cancelPayment(id);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get payment history", description = "Retrieve complete transaction history for a payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public List<PaymentHistory> history(
            @Parameter(description = "Payment ID", example = "12345", required = true)
            @PathVariable Long id) {
        return orchestrationService.history(id);
    }
}
