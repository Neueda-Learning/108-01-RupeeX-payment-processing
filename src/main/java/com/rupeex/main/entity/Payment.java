package com.rupeex.main.entity;


import com.rupeex.main.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(
                        name = "idx_payment_reference",
                        columnList = "payment_reference"
                ),
                @Index(
                        name = "idx_idempotency_key",
                        columnList = "idempotency_key"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(
            name = "payment_reference",
            unique = true,
            nullable = false
    )
    private String paymentReference;



    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;



    @Column(nullable = false)
    private String currency;



    @Column(nullable = false)
    private String sourceAccount;



    @Column(nullable = false)
    private String destinationAccount;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;



    @Column(
            name = "idempotency_key",
            unique = true
    )
    private String idempotencyKey;



    private String errorCode;


    private String errorMessage;



    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;



    private LocalDateTime updatedAt;



    /*
       Automatically called before saving first time
    */
    @PrePersist
    public void prePersist(){


        LocalDateTime now = LocalDateTime.now();


        this.createdAt = now;

        this.updatedAt = now;



        if(this.status == null){

            this.status = PaymentStatus.INITIATED;

        }

    }




    /*
       Automatically called before update
    */
    @PreUpdate
    public void preUpdate(){


        this.updatedAt = LocalDateTime.now();

    }




    /*
       Getter methods
       Used by Service, Controller, DTO mapping
    */


    public Long getId(){

        return this.id;

    }



    public BigDecimal getAmount(){

        return this.amount;

    }




    public String getCurrency(){

        return this.currency;

    }




    public PaymentStatus getStatus(){

        return this.status;

    }




    public String getPaymentReference(){

        return this.paymentReference;

    }




    public LocalDateTime getCreatedAt(){

        return this.createdAt;

    }




    /*
       Setter methods
       Used by Service layer
    */


    public void setAmount(BigDecimal amount){

        this.amount = amount;

    }




    public void setStatus(PaymentStatus status){

        this.status = status;

    }





    public void setCurrency(String currency){

        this.currency = currency;

    }





    public void setPaymentReference(String paymentReference){

        this.paymentReference = paymentReference;

    }





    /*
       Business methods
       Used during payment processing
    */


    public void markAsProcessing(){

        this.status = PaymentStatus.PROCESSING;

    }




    public void markAsSuccess(){

        this.status = PaymentStatus.SUCCESS;

        this.errorCode = null;

        this.errorMessage = null;

    }




    public void markAsFailed(
            String errorCode,
            String errorMessage
    ){

        this.status = PaymentStatus.FAILED;

        this.errorCode = errorCode;

        this.errorMessage = errorMessage;

    }




    public boolean isSuccessful(){

        return this.status == PaymentStatus.SUCCESS;

    }




    public boolean isFailed(){

        return this.status == PaymentStatus.FAILED;

    }




    public boolean isProcessing(){

        return this.status == PaymentStatus.PROCESSING;

    }



}



//package com.rupeex.main.entity;
//
//
//import com.rupeex.main.enums.PaymentStatus;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//
//@Entity
//@Table(
//        name = "payments",
//        indexes = {
//                @Index(
//                        name = "idx_payment_reference",
//                        columnList = "payment_reference"
//                ),
//                @Index(
//                        name = "idx_idempotency_key",
//                        columnList = "idempotency_key"
//                )
//        }
//)
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class Payment {
//
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//
//    @Column(
//            name = "payment_reference",
//            unique = true,
//            nullable = false
//    )
//    private String paymentReference;
//
//
//
//    @Column(
//            nullable = false,
//            precision = 19,
//            scale = 2
//    )
//    private BigDecimal amount;
//
//
//
//    @Column(nullable = false)
//    private String currency;
//
//
//
//    @Column(nullable = false)
//    private String sourceAccount;
//
//
//
//    @Column(nullable = false)
//    private String destinationAccount;
//
//
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private PaymentStatus status;
//
//
//
//    @Column(
//            name="idempotency_key",
//            unique=true
//    )
//    private String idempotencyKey;
//
//
//
//    private String errorCode;
//
//
//    private String errorMessage;
//
//
//
//    @Column(nullable=false,
//            updatable=false)
//    private LocalDateTime createdAt;
//
//
//
//    private LocalDateTime updatedAt;
//
//
//
//    @PrePersist
//    public void prePersist(){
//
//        LocalDateTime now = LocalDateTime.now();
//
//        createdAt = now;
//        updatedAt = now;
//
//
//        if(status == null){
//            status = PaymentStatus.INITIATED;
//        }
//
//    }
//
//
//
//    @PreUpdate
//    public void preUpdate(){
//
//        updatedAt = LocalDateTime.now();
//
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public Long getAmount() {
//        return amount.longValue();
//    }
//
//    public Object getCurrency() {
//        return currency;
//    }
//
//    public Object getStatus() {
//        return status;
//    }
//
//    public void setAmount(Object amount) {
//
//    }
//
//    public void setStatus(PaymentStatus paymentStatus) {
//
//    }
//}



