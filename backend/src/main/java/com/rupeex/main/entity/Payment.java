package com.rupeex.main.entity;


import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.util.DateTimeUtil;

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
    @Column(nullable = false, length = 50)
    private PaymentStatus status;



    @Column(
            name = "idempotency_key",
            unique = true
    )
    private String idempotencyKey;



    @Column(
            name = "payer_email",
            nullable = true,
            length = 255
    )
    private String payerEmail;



    private String errorCode;


    private String errorMessage;



    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;



    private LocalDateTime updatedAt;




    @Column
    private String sourceCurrency;


    @Column
    private String destinationCurrency;


    @Column
    private BigDecimal convertedAmount;


    @Column
    private BigDecimal exchangeRate;


    /**
     * When set to a future IST timestamp, the payment is held in the
     * {@code SCHEDULED} status by {@link com.rupeex.main.platform.service.PaymentOrchestrationService}
     * instead of immediately entering the fraud/risk pipeline. A background
     * scheduler promotes it once this time is reached.
     */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;


    /**
     * Origin country of the payer, captured at creation time so it can be
     * re-used by the fraud engine when a scheduled payment is later released
     * into the pipeline (the original request is not retained).
     */
    @Column(name = "origin_country", length = 8)
    private String originCountry;


    /**
     * Destination country of the payee, captured at creation time for
     * cross-border payment tracking and fraud detection.
     */
    @Column(name = "destination_country", length = 8)
    private String destinationCountry;



    /*
       Automatically called before saving first time
    */
    @PrePersist
    public void prePersist(){


        LocalDateTime now = DateTimeUtil.nowIst();


        this.createdAt = now;

        this.updatedAt = now;



        if(this.status == null){

            this.status = PaymentStatus.CREATED;

        }

    }




    /*
       Automatically called before update
    */
    @PreUpdate
    public void preUpdate(){


        this.updatedAt = DateTimeUtil.nowIst();

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



    public String getSourceAccount(){

        return this.sourceAccount;

    }



    public String getDestinationAccount(){

        return this.destinationAccount;

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



    public LocalDateTime getUpdatedAt(){

        return this.updatedAt;

    }



    public String getErrorCode(){

        return this.errorCode;

    }



    public String getErrorMessage(){

        return this.errorMessage;

    }




    public String getPayerEmail(){

        return this.payerEmail;

    }



    public LocalDateTime getScheduledAt(){

        return this.scheduledAt;

    }



    public String getOriginCountry(){

        return this.originCountry;

    }




    public String getDestinationCountry(){

        return this.destinationCountry;

    }




    public String getSourceCurrency(){

        return this.sourceCurrency;

    }



    public void setSourceCurrency(String sourceCurrency){

        this.sourceCurrency = sourceCurrency;

    }



    public String getDestinationCurrency(){

        return this.destinationCurrency;

    }



    public void setDestinationCurrency(String destinationCurrency){

        this.destinationCurrency = destinationCurrency;

    }



    public BigDecimal getConvertedAmount(){

        return this.convertedAmount;

    }



    public void setConvertedAmount(BigDecimal convertedAmount){

        this.convertedAmount = convertedAmount;

    }



    public BigDecimal getExchangeRate(){

        return this.exchangeRate;

    }



    public void setExchangeRate(BigDecimal exchangeRate){

        this.exchangeRate = exchangeRate;

    }



    public void setOriginCountry(String originCountry){

        this.originCountry = originCountry;

    }



    public void setDestinationCountry(String destinationCountry){

        this.destinationCountry = destinationCountry;

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



    public void setSourceAccount(String sourceAccount){

        this.sourceAccount = sourceAccount;

    }



    public void setDestinationAccount(String destinationAccount){

        this.destinationAccount = destinationAccount;

    }




    public void setPaymentReference(String paymentReference){

        this.paymentReference = paymentReference;

    }



    public void setIdempotencyKey(String idempotencyKey){

        this.idempotencyKey = idempotencyKey;

    }



    public void setPayerEmail(String payerEmail){

        this.payerEmail = payerEmail;

    }




    public void setScheduledAt(LocalDateTime scheduledAt){

        this.scheduledAt = scheduledAt;

    }



    /*
       Business methods
       Used during payment processing
    */


    public void markAsProcessing(){

        this.status = PaymentStatus.PROCESSING;

    }




    public void markAsSuccess(){

        this.status = PaymentStatus.SETTLED;

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
