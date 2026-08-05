package com.rupeex.main.dto;


import com.rupeex.main.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;


public class StatusUpdateRequest {


    @NotNull(message = "Status is required")
    private PaymentStatus status;


    private String reason;



    // Getters and Setters

    public PaymentStatus getStatus() {

        return status;

    }


    public void setStatus(PaymentStatus status) {

        this.status = status;

    }


    public String getReason() {

        return reason;

    }


    public void setReason(String reason) {

        this.reason = reason;

    }

}