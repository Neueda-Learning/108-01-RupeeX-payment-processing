package com.rupeex.main.entity;

import com.rupeex.main.enums.VerificationStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_verifications")
public class PaymentVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long paymentId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, unique = true)
    private String verificationToken;

    @Column(nullable = false)
    private double trustScoreAtDecision;

    @Column(nullable = false)
    private boolean currencyChangeTriggered;

    @Column(nullable = false)
    private boolean largePaymentTriggered;

    @Column(nullable = false)
    private boolean rapidPaymentsTriggered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    private String customerEmail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public double getTrustScoreAtDecision() {
        return trustScoreAtDecision;
    }

    public void setTrustScoreAtDecision(double trustScoreAtDecision) {
        this.trustScoreAtDecision = trustScoreAtDecision;
    }

    public boolean isCurrencyChangeTriggered() {
        return currencyChangeTriggered;
    }

    public void setCurrencyChangeTriggered(boolean currencyChangeTriggered) {
        this.currencyChangeTriggered = currencyChangeTriggered;
    }

    public boolean isLargePaymentTriggered() {
        return largePaymentTriggered;
    }

    public void setLargePaymentTriggered(boolean largePaymentTriggered) {
        this.largePaymentTriggered = largePaymentTriggered;
    }

    public boolean isRapidPaymentsTriggered() {
        return rapidPaymentsTriggered;
    }

    public void setRapidPaymentsTriggered(boolean rapidPaymentsTriggered) {
        this.rapidPaymentsTriggered = rapidPaymentsTriggered;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
}

