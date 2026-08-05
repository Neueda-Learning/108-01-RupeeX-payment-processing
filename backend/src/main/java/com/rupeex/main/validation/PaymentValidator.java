package com.rupeex.main.validation;


import com.rupeex.main.dto.PaymentRequest;

import com.rupeex.main.exception.InvalidPaymentException;


import org.springframework.stereotype.Component;


import java.math.BigDecimal;

import java.util.Set;



@Component
public class PaymentValidator {



    private static final Set<String>
            SUPPORTED_CURRENCIES =
            Set.of(
                    "INR",
                    "USD",
                    "EUR",
                    "GBP"
            );



    public void validate(
            PaymentRequest request
    ){


        validateAmount(
                request.getAmount()
        );


        validateCurrency(
                request.getCurrency()
        );


        validateAccounts(
                request.getSourceAccount(),
                request.getDestinationAccount()
        );


    }





    private void validateAmount(
            BigDecimal amount
    ){


        if(amount == null ||
                amount.compareTo(
                        BigDecimal.ZERO
                ) <= 0){


            throw new InvalidPaymentException(
                    "Amount must be greater than zero"
            );

        }

    }





    private void validateCurrency(
            String currency
    ){


        if(!SUPPORTED_CURRENCIES
                .contains(currency)){


            throw new InvalidPaymentException(
                    "Currency not supported"
            );

        }

    }





    private void validateAccounts(
            String source,
            String destination
    ){


        if(source.equals(destination)){


            throw new InvalidPaymentException(
                    "Source and destination account cannot be same"
            );

        }


    }


}