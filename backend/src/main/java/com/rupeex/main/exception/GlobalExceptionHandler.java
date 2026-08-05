package com.rupeex.main.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;



@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(
            PaymentNotFoundException.class
    )
    public ProblemDetail
    handlePaymentNotFound(
            PaymentNotFoundException ex
    ){

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Payment Not Found");
        problemDetail.setType(URI.create("https://rupeex.dev/problems/payment-not-found"));
        problemDetail.setProperty("errorCode", "PAYMENT_NOT_FOUND");
        return problemDetail;

    }



    @ExceptionHandler(
            DuplicatePaymentException.class
    )
    public ProblemDetail
    handleDuplicatePayment(
            DuplicatePaymentException ex
    ){

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Duplicate Payment");
        problemDetail.setType(URI.create("https://rupeex.dev/problems/duplicate-payment"));
        problemDetail.setProperty("errorCode", "DUPLICATE_PAYMENT");
        return problemDetail;

    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationError(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed"
        );
        problemDetail.setTitle("Invalid Request");
        problemDetail.setType(URI.create("https://rupeex.dev/problems/validation"));

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }




    @ExceptionHandler(Exception.class)
    public ProblemDetail
    handleGeneralException(
            Exception ex
    ){

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
        problemDetail.setTitle("Internal Error");
        problemDetail.setType(URI.create("https://rupeex.dev/problems/internal-error"));
        problemDetail.setProperty("errorCode", "INTERNAL_ERROR");
        return problemDetail;

    }


}