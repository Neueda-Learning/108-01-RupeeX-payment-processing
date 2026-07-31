package com.rupeex.main.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;



@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(
            PaymentNotFoundException.class
    )
    public ResponseEntity<?>
    handlePaymentNotFound(
            PaymentNotFoundException ex
    ){


        Map<String,Object> response =
                new HashMap<>();


        response.put(
                "errorCode",
                "PAYMENT_NOT_FOUND"
        );


        response.put(
                "message",
                ex.getMessage()
        );


        response.put(
                "timestamp",
                LocalDateTime.now()
        );


        return new ResponseEntity<>(
                response,
                HttpStatus.NOT_FOUND
        );

    }



    @ExceptionHandler(
            DuplicatePaymentException.class
    )
    public ResponseEntity<?>
    handleDuplicatePayment(
            DuplicatePaymentException ex
    ){


        Map<String,Object> response =
                new HashMap<>();


        response.put(
                "errorCode",
                "DUPLICATE_PAYMENT"
        );


        response.put(
                "message",
                ex.getMessage()
        );


        response.put(
                "timestamp",
                LocalDateTime.now()
        );


        return new ResponseEntity<>(
                response,
                HttpStatus.CONFLICT
        );

    }




    @ExceptionHandler(Exception.class)
    public ResponseEntity<?>
    handleGeneralException(
            Exception ex
    ){


        Map<String,Object> response =
                new HashMap<>();


        response.put(
                "errorCode",
                "INTERNAL_ERROR"
        );


        response.put(
                "message",
                ex.getMessage()
        );


        response.put(
                "timestamp",
                LocalDateTime.now()
        );


        return new ResponseEntity<>(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR
        );

    }


}