package com.rupeex.onboarding.dto;

import com.rupeex.onboarding.enums.OnboardingStatus;
import com.rupeex.onboarding.enums.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerResponse {

    private UUID customerId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dob;
    private String externalRef;
    private OnboardingStatus status;
    private String accountNumber;
    private String accountType;
    private String currency;
    private String countryCode;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public OnboardingStatus getStatus() { return status; }
    public void setStatus(OnboardingStatus status) { this.status = status; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

