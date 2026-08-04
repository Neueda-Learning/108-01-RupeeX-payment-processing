package com.rupeex.onboarding.dto;

import com.rupeex.onboarding.enums.OnboardingStatus;
import com.rupeex.onboarding.enums.UserRole;

import java.util.UUID;

public class CustomerSummary {

    private UUID customerId;
    private String fullName;
    private String email;
    private String phone;
    private String accountNumber;
    private OnboardingStatus status;
    private UserRole role;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public OnboardingStatus getStatus() { return status; }
    public void setStatus(OnboardingStatus status) { this.status = status; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}

