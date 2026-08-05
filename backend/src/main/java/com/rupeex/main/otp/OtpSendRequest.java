package com.rupeex.main.otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class OtpSendRequest {

    @NotBlank
    @Email
    private String email;

    /** Source account number — used to resolve the account holder name for the email greeting. */
    @NotBlank
    private String sourceAccount;

    public OtpSendRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }
}
