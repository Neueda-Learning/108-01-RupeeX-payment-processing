package com.rupeex.main.service.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.rupeex.main.client.ExchangeApiClient;
import com.rupeex.main.dto.ExchangeRequest;
import com.rupeex.main.dto.ExchangeResponse;
import com.rupeex.main.service.ExchangeRateService;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.RoundingMode;



@Service
public class ExchangeRateServiceImpl
        implements ExchangeRateService {



    private final ExchangeApiClient client;

    private final ObjectMapper objectMapper;



    public ExchangeRateServiceImpl(
            ExchangeApiClient client,
            ObjectMapper objectMapper){

        this.client = client;
        this.objectMapper = objectMapper;

    }



    @Override
    public ExchangeResponse convert(
            ExchangeRequest request){


        try{


            String response =
                    client.getRates(
                            request.getFromCurrency()
                    );


            JsonNode json =
                    objectMapper.readTree(response);



            BigDecimal rate = extractRate(
                    json,
                    request.getToCurrency()
            );



            BigDecimal converted =
                    request
                            .getAmount()
                            .multiply(
                                    rate
                            )
                            .setScale(2, RoundingMode.HALF_UP);



            return ExchangeResponse.builder()

                    .originalAmount(request.getAmount())

                    .fromCurrency(request.getFromCurrency())

                    .toCurrency(request.getToCurrency())

                    .exchangeRate(
                            rate
                    )

                    .convertedAmount(converted)

                    .build();



        }catch(Exception e){

            throw new RuntimeException(
                    "Currency conversion failed",
                    e
            );

        }



    }


    private BigDecimal extractRate(
            JsonNode responseJson,
            String toCurrency) {

        JsonNode ratesNode = responseJson.get("conversion_rates");

        if (ratesNode == null || ratesNode.isNull()) {
            throw new IllegalStateException("Exchange API response does not contain conversion_rates");
        }

        JsonNode targetRateNode = ratesNode.get(toCurrency);

        if (targetRateNode == null || targetRateNode.isNull()) {
            throw new IllegalArgumentException("Unsupported target currency: " + toCurrency);
        }

        return targetRateNode.decimalValue();
    }



}