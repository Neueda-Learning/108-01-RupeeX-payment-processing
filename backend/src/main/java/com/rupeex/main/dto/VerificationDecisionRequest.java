package com.rupeex.main.dto;

import jakarta.validation.constraints.NotBlank;

public class VerificationDecisionRequest {

    @NotBlank(message = "Verification token is required")
    private String token;

    private boolean approved;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}

