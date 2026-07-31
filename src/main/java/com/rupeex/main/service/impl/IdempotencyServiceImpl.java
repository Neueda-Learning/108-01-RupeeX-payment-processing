package com.rupeex.main.service.impl;

import com.rupeex.main.repository.PaymentRepository;

@Service
public class IdempotencyServiceImpl
        implements IdempotencyService{


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