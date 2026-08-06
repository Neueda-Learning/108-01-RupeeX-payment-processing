package com.rupeex.main.service.impl;

import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.service.PaymentStatusService;
import org.springframework.stereotype.Service;

@Service
public class PaymentStatusServiceImpl
        implements PaymentStatusService {


    @Override
    public void updateStatus(Long paymentId, PaymentStatus newStatus) {

        System.out.println("Updating payment ID " + paymentId + " to new status: " + newStatus);
    }

    @Override
    public boolean isValidTransition(
            PaymentStatus oldStatus,
            PaymentStatus newStatus){


        return switch(oldStatus){

            case CREATED ->
                    newStatus == PaymentStatus.VALIDATED ||
                            newStatus == PaymentStatus.FAILED;


            case VALIDATED ->
                    newStatus == PaymentStatus.SENT ||
                            newStatus == PaymentStatus.FAILED;


            case SENT ->
                    newStatus == PaymentStatus.COMPLETED ||
                            newStatus == PaymentStatus.FAILED;


            default ->
                    false;

        };

    }


}