package com.rupeex.main.service.impl;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.exception.InvalidPaymentException;
import com.rupeex.main.service.PaymentValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentValidationServiceImpl
        implements PaymentValidationService {


    @Override
    public void validate(
            PaymentRequest request){


        if(request.getAmount()
                .compareTo(BigDecimal.ZERO)<=0){

            throw new InvalidPaymentException(
                    "Invalid amount"
            );

        }


        if(request.getSourceAccount()
                .equals(request.getDestinationAccount())){

            throw new InvalidPaymentException(
                    "Same account"
            );

        }


    }


}