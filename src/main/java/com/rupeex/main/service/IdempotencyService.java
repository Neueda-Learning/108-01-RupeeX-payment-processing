package com.rupeex.main.service;

public interface IdempotencyService {


    void checkDuplicate(
            String key
    );


}