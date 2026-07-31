package com.rupeex.main.dto;


import com.rupeex.main.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;


public class StatusUpdateRequest {


    @NotNull(message = "Status is required")
    private PaymentStatus status;


    private String reason;



    // Getters and Setters

}