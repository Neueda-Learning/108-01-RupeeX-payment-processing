package com.rupeex.main.scheduler;


import com.rupeex.main.entity.Payment;

import com.rupeex.main.enums.PaymentStatus;

import com.rupeex.main.repository.PaymentRepository;

import com.rupeex.main.service.PaymentProcessingService;


import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;


import java.util.List;



@Component
public class PaymentRetryScheduler {



    private final PaymentRepository paymentRepository;


    private final PaymentProcessingService
            processingService;



    public PaymentRetryScheduler(
            PaymentRepository paymentRepository,
            PaymentProcessingService processingService
    ){

        this.paymentRepository =
                paymentRepository;

        this.processingService =
                processingService;

    }




    @Scheduled(
            fixedDelay = 300000
    )
    public void retryFailedPayments(){


        List<Payment> failedPayments =
                paymentRepository
                        .findByStatus(
                                PaymentStatus.FAILED
                        );



        for(Payment payment :
                failedPayments){


            processingService
                    .processPayment(
                            payment.getId()
                    );

        }


    }


}