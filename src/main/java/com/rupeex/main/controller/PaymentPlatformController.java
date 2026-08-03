package com.rupeex.main.controller;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.platform.dto.PaymentPlatformRequest;
import com.rupeex.main.platform.dto.PaymentPlatformResponse;
import com.rupeex.main.platform.service.PaymentOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentPlatformController {

    private final PaymentOrchestrationService orchestrationService;

    public PaymentPlatformController(PaymentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentPlatformResponse createPayment(@Valid @RequestBody PaymentPlatformRequest request) {
        return orchestrationService.createPayment(request);
    }

    @GetMapping
    public Page<Payment> getPayments(Pageable pageable) {
        return orchestrationService.getPayments(pageable);
    }

    @GetMapping("/{id}")
    public PaymentPlatformResponse getPayment(@PathVariable Long id) {
        return orchestrationService.getPayment(id);
    }

    @PostMapping("/{id}/retry")
    public PaymentPlatformResponse retry(@PathVariable Long id) {
        return orchestrationService.retryPayment(id);
    }

    @PostMapping("/{id}/cancel")
    public PaymentPlatformResponse cancel(@PathVariable Long id) {
        return orchestrationService.cancelPayment(id);
    }

    @GetMapping("/{id}/history")
    public List<PaymentHistory> history(@PathVariable Long id) {
        return orchestrationService.history(id);
    }
}
