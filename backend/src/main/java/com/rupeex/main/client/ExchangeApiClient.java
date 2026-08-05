package com.rupeex.main.client;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;


@Component
public class ExchangeApiClient {


    private final RestClient restClient;

    private final String apiKey;



    public ExchangeApiClient(
            RestClient.Builder builder,
            @Value("${exchange.api.url:https://v6.exchangerate-api.com/v6}") String apiUrl,
            @Value("${exchange.api.key:demo-key}") String apiKey){

        this.restClient =
                builder
                        .baseUrl(apiUrl)
                        .build();

        this.apiKey = apiKey;

    }



    public String getRates(String currency){


        return restClient.get()

                .uri("/{apiKey}/latest/{currency}", apiKey, currency)

                .accept(MediaType.APPLICATION_JSON)

                .retrieve()

                .body(String.class);


    }


}