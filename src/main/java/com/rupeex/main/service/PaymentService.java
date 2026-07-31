package com.rupeex.main.service;


import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.dto.PaymentResponse;
import com.rupeex.main.enums.PaymentStatus;


public interface PaymentService {


    PaymentResponse createPayment(
            PaymentRequest request
    );


    PaymentResponse getPaymentById(
            Long paymentId
    );


    void updatePaymentStatus(
            Long paymentId,
            PaymentStatus status
    );


}