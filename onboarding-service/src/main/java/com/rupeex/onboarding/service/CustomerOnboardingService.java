package com.rupeex.onboarding.service;

import com.rupeex.onboarding.dto.ConsentRequest;
import com.rupeex.onboarding.dto.CreateCustomerRequest;
import com.rupeex.onboarding.dto.CustomerResponse;
import com.rupeex.onboarding.dto.CustomerStatusResponse;
import com.rupeex.onboarding.dto.RejectRequest;

import java.util.UUID;

public interface CustomerOnboardingService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    CustomerResponse getCustomer(UUID customerId);

    CustomerStatusResponse getStatus(UUID customerId);

    void addConsent(UUID customerId, ConsentRequest request);

    void submitForReview(UUID customerId);

    void approve(UUID customerId);

    void reject(UUID customerId, RejectRequest request);
}

