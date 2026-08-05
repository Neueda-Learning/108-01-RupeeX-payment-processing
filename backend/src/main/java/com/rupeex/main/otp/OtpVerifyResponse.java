package com.rupeex.main.otp;

public class OtpVerifyResponse {

    private boolean valid;
    private String message;

    public OtpVerifyResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}
