package com.rupeex.onboarding.controller;

import com.rupeex.onboarding.dto.ConsentRequest;
import com.rupeex.onboarding.dto.CreateCustomerRequest;
import com.rupeex.onboarding.dto.CustomerResponse;
import com.rupeex.onboarding.dto.CustomerStatusResponse;
import com.rupeex.onboarding.dto.CustomerSummary;
import com.rupeex.onboarding.dto.RejectRequest;
import com.rupeex.onboarding.service.CustomerOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
public class CustomerOnboardingController {

    private final CustomerOnboardingService customerOnboardingService;

    public CustomerOnboardingController(CustomerOnboardingService customerOnboardingService) {
        this.customerOnboardingService = customerOnboardingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return customerOnboardingService.createCustomer(request);
    }

    @GetMapping
    public List<CustomerSummary> listCustomers() {
        return customerOnboardingService.listCustomers();
    }

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable UUID customerId) {
        return customerOnboardingService.getCustomer(customerId);
    }

    @GetMapping("/{customerId}/status")
    public CustomerStatusResponse getStatus(@PathVariable UUID customerId) {
        return customerOnboardingService.getStatus(customerId);
    }

    @PostMapping("/{customerId}/consents")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addConsent(@PathVariable UUID customerId, @Valid @RequestBody ConsentRequest request) {
        customerOnboardingService.addConsent(customerId, request);
    }

    @PostMapping("/{customerId}/submit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitForReview(@PathVariable UUID customerId) {
        customerOnboardingService.submitForReview(customerId);
    }

    @PostMapping("/{customerId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable UUID customerId) {
        customerOnboardingService.approve(customerId);
    }

    @PostMapping("/{customerId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID customerId, @Valid @RequestBody RejectRequest request) {
        customerOnboardingService.reject(customerId, request);
    }
}

