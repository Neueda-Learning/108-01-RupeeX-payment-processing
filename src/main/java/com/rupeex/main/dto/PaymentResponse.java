package com.rupeex.main.dto;

import com.rupeex.main.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long paymentId;
    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private String sourceAccount;
    private String destinationAccount;
    private PaymentStatus status;
    private String errorCode;
    private String errorMessage;
    private Double trustScore;
    private boolean verificationRequired;
    private String verificationToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // Payment ID Getter Setter

    public Long getPaymentId() {

        return paymentId;

    }


    public void setPaymentId(Long paymentId) {

        this.paymentId = paymentId;

    }



    // Payment Reference Getter Setter

    public String getPaymentReference() {

        return paymentReference;

    }


    public void setPaymentReference(String paymentReference) {

        this.paymentReference = paymentReference;

    }



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



    // Status Getter Setter

    public PaymentStatus getStatus() {

        return status;

    }


    public void setStatus(PaymentStatus status) {

        this.status = status;

    }



    // Error Code Getter Setter

    public String getErrorCode() {

        return errorCode;

    }


    public void setErrorCode(String errorCode) {

        this.errorCode = errorCode;

    }



    // Error Message Getter Setter

    public String getErrorMessage() {

        return errorMessage;

    }


    public void setErrorMessage(String errorMessage) {

        this.errorMessage = errorMessage;

    }



    // Created At Getter Setter

    public LocalDateTime getCreatedAt() {

        return createdAt;

    }


    public void setCreatedAt(LocalDateTime createdAt) {

        this.createdAt = createdAt;

    }



    // Updated At Getter Setter

    public LocalDateTime getUpdatedAt() {

        return updatedAt;

    }


    public void setUpdatedAt(LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;

    }


    public Double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(double trustScore) {
        this.trustScore = trustScore;
    }

    public boolean isVerificationRequired() {
        return verificationRequired;
    }

    public void setVerificationRequired(boolean requiresVerification) {
        this.verificationRequired = requiresVerification;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }
}