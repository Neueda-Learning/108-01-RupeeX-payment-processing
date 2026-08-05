package com.rupeex.main.controller;


import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.service.PaymentAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/audit")
@Tag(name = "Payment Audit", description = "Payment history and audit logging")
public class PaymentAuditController {


    private final PaymentAuditService paymentAuditService;


    public PaymentAuditController(
            PaymentAuditService paymentAuditService) {

        this.paymentAuditService = paymentAuditService;
    }



    // Get payment audit logs
    @GetMapping("/payments/{paymentId}/logs")
    @Operation(summary = "Get payment history", description = "Retrieve complete audit trail and history logs for a specific payment")
    @ApiResponse(responseCode = "200", description = "Payment history retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentHistory.class)))
    public List<PaymentHistory> getPaymentHistory(
            @Parameter(description = "Payment ID", example = "12345", required = true)
            @PathVariable Long paymentId) {


        return paymentAuditService
                .getHistory(paymentId);
    }

}