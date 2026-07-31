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





    // Amount Getter Setter

    public BigDecimal getAmount() {

        return amount;

    }


    public void setAmount(BigDecimal amount) {

        this.amount = amount;

    }





    // Currency Getter Setter

    public String getCurrency() {

        return currency;

    }


    public void setCurrency(String currency) {

        this.currency = currency;

    }





    // Source Account Getter Setter

    public String getSourceAccount() {

        return sourceAccount;

    }


    public void setSourceAccount(String sourceAccount) {

        this.sourceAccount = sourceAccount;

    }





    // Destination Account Getter Setter

    public String getDestinationAccount() {

        return destinationAccount;

    }


    public void setDestinationAccount(String destinationAccount) {

        this.destinationAccount = destinationAccount;

    }





    // Idempotency Key Getter Setter

    public String getIdempotencyKey() {

        return idempotencyKey;

    }


    public void setIdempotencyKey(String idempotencyKey) {

        this.idempotencyKey = idempotencyKey;

    }


}