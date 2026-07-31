package com.rupeex.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

public class ConsentRequest {

    @NotBlank(message = "consentType is required")
    private String consentType;

    @NotBlank(message = "version is required")
    private String version;

    private boolean accepted;

    public String getConsentType() {
        return consentType;
    }

    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}

