package com.rupeex.main.entity;


import com.rupeex.main.enums.PaymentStatus;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name="payment_status_history")
@Data
public class PaymentHistory {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long paymentId;


    @Enumerated(EnumType.STRING)
    private PaymentStatus oldStatus;


    @Enumerated(EnumType.STRING)
    private PaymentStatus newStatus;


    private String reason;


    private LocalDateTime changedAt;



    @PrePersist
    public void prePersist(){

        changedAt = LocalDateTime.now();

    }

}