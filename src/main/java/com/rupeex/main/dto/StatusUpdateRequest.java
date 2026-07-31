package com.rupeex.main.dto;


import jakarta.validation.constraints.NotNull;


public class StatusUpdateRequest {


    @NotNull(message = "Status is required")
    private PaymentStatus status;


    private String reason;



    // Getters and Setters

}