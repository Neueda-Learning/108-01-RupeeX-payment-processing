package com.rupeex.main.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentRequest {


    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Amount must be greater than zero"
    )
    private BigDecimal amount;


    @NotBlank(message = "Currency is required")
    private String currency;


    @NotBlank(message = "Source account is required")
    private String sourceAccount;


    @NotBlank(message = "Destination account is required")
    private String destinationAccount;


    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;



    // Getters and Setters

}