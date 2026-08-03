package com.rupeex.main.entity;


import com.rupeex.main.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "payment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistory {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(nullable = false)
    private Long paymentId;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus oldStatus;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus newStatus;



    private String reason;



    @Column(nullable = false)
    private LocalDateTime changedAt;




    @PrePersist
    public void prePersist(){

        changedAt = LocalDateTime.now();

    }





    // Explicit getters and setters
    // Useful for other classes


    public Long getId(){

        return id;

    }



    public Long getPaymentId(){

        return paymentId;

    }



    public void setPaymentId(Long paymentId){

        this.paymentId = paymentId;

    }



    public PaymentStatus getOldStatus(){

        return oldStatus;

    }



    public void setOldStatus(PaymentStatus oldStatus){

        this.oldStatus = oldStatus;

    }



    public PaymentStatus getNewStatus(){

        return newStatus;

    }



    public void setNewStatus(PaymentStatus newStatus){

        this.newStatus = newStatus;

    }



    public String getReason(){

        return reason;

    }



    public void setReason(String reason){

        this.reason = reason;

    }



    public LocalDateTime getChangedAt(){

        return changedAt;

    }



    public void setChangedAt(LocalDateTime changedAt){

        this.changedAt = changedAt;

    }

}