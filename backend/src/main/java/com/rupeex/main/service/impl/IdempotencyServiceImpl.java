package com.rupeex.main.service.impl;

import com.rupeex.main.exception.DuplicatePaymentException;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyServiceImpl
        implements IdempotencyService {


    @Autowired
    private PaymentRepository repository;



    @Override
    public void checkDuplicate(String key){


        boolean exists =
                repository.existsByIdempotencyKey(key);



        if(exists){

            throw new DuplicatePaymentException(
                    "Payment already exists"
            );

        }


    }


}