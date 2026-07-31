package com.rupeex.main.service.impl;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.dto.PaymentResponse;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.service.IdempotencyService;
import com.rupeex.main.service.PaymentService;
import com.rupeex.main.service.PaymentValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl
        implements PaymentService {


    @Autowired
    private PaymentRepository paymentRepository;


    @Autowired
    private PaymentValidationService validationService;


    @Autowired
    private IdempotencyService idempotencyService;



    @Override
    public PaymentResponse createPayment(
            PaymentRequest request){


        idempotencyService.checkDuplicate(
                request.getIdempotencyKey()
        );


        validationService.validate(request);



        Payment payment = new Payment();

        payment.setAmount(request.getAmount());

        payment.setCurrency(request.getCurrency());

        payment.setStatus(
                PaymentStatus.CREATED
        );



        Payment saved =
                paymentRepository.save(payment);



        return mapToResponse(saved);

    }

}