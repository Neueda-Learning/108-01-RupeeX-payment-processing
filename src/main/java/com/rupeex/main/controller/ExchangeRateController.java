package com.rupeex.main.controller;


import com.rupeex.main.dto.ExchangeRequest;
import com.rupeex.main.dto.ExchangeResponse;

import com.rupeex.main.service.ExchangeRateService;


import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/exchange")
public class ExchangeRateController {



    private final ExchangeRateService service;



    public ExchangeRateController(
            ExchangeRateService service){

        this.service=service;

    }



    @PostMapping("/convert")
    public ExchangeResponse convert(
            @RequestBody ExchangeRequest request){

        return service.convert(request);

    }


}