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


    private LocalDateTime createdAt;


    private LocalDateTime updatedAt;

    public void setPaymentId(Long id) {
    }

    public void setCurrency(Object currency) {
        
    }

    public void setStatus(Object status) {
    }


    // Getters and Setters

}