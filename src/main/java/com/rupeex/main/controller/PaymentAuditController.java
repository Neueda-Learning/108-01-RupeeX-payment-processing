package com.rupeex.main.controller;


import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.service.PaymentAuditService;

import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/payments")
public class PaymentAuditController {


    private final PaymentAuditService paymentAuditService;


    public PaymentAuditController(
            PaymentAuditService paymentAuditService) {

        this.paymentAuditService = paymentAuditService;
    }



    // Get payment history
    @GetMapping("/{paymentId}/history")
    public List<PaymentHistory> getPaymentHistory(
            @PathVariable Long paymentId) {


        return paymentAuditService
                .getHistory(paymentId);
    }

}