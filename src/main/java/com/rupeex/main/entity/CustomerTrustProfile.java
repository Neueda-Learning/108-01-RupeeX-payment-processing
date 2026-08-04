package com.rupeex.main.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_trust_profiles")
public class CustomerTrustProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String customerId;

    @Column(nullable = false)
    private double currencyChangeScore;

    @Column(nullable = false)
    private double largePaymentScore;

    @Column(nullable = false)
    private double rapidPaymentsScore;

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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public double getCurrencyChangeScore() {
        return currencyChangeScore;
    }

    public void setCurrencyChangeScore(double currencyChangeScore) {
        this.currencyChangeScore = currencyChangeScore;
    }

    public double getLargePaymentScore() {
        return largePaymentScore;
    }

    public void setLargePaymentScore(double largePaymentScore) {
        this.largePaymentScore = largePaymentScore;
    }

    public double getRapidPaymentsScore() {
        return rapidPaymentsScore;
    }

    public void setRapidPaymentsScore(double rapidPaymentsScore) {
        this.rapidPaymentsScore = rapidPaymentsScore;
    }
}

