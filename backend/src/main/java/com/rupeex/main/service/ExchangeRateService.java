package com.rupeex.main.service;


import com.rupeex.main.dto.ExchangeRequest;
import com.rupeex.main.dto.ExchangeResponse;

public interface ExchangeRateService {


    ExchangeResponse convert(
            ExchangeRequest request
    );


}