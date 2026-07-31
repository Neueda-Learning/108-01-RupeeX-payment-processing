package com.rupeex.main.entity;


import com.rupeex.main.enums.PaymentStatus;

import jakarta.persistence.*;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "payments")
@Data
public class Payment {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "payment_reference",
            unique = true,
            nullable = false)
    private String paymentReference;


    @Column(nullable = false)
    private BigDecimal amount;


    @Column(nullable = false)
    private String currency;


    private String sourceAccount;


    private String destinationAccount;


    @Enumerated(EnumType.STRING)
    private PaymentStatus status;


    @Column(unique = true)
    private String idempotencyKey;


    private String errorCode;


    private String errorMessage;


    private LocalDateTime createdAt;


    private LocalDateTime updatedAt;


    @PrePersist
    public void prePersist(){

        createdAt = LocalDateTime.now();

        updatedAt = LocalDateTime.now();

    }


    @PreUpdate
    public void preUpdate(){

        updatedAt = LocalDateTime.now();

    }

}