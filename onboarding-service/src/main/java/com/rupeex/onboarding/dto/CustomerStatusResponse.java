package com.rupeex.onboarding.dto;

import com.rupeex.onboarding.enums.OnboardingStatus;

import java.util.UUID;

public class CustomerStatusResponse {

    private UUID customerId;
    private OnboardingStatus status;
    private boolean eligibleForPayments;

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public OnboardingStatus getStatus() {
        return status;
    }

    public void setStatus(OnboardingStatus status) {
        this.status = status;
    }

    public boolean isEligibleForPayments() {
        return eligibleForPayments;
    }

    public void setEligibleForPayments(boolean eligibleForPayments) {
        this.eligibleForPayments = eligibleForPayments;
    }
}

