package com.rupeex.main.service;

import com.rupeex.main.dto.PaymentRequest;

public interface PaymentValidationService {


    void validate(
            PaymentRequest request
    );


}