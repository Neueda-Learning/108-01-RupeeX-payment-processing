package com.rupeex.main.service;

import com.rupeex.main.enums.PaymentStatus;

public interface PaymentStatusService {


    void updateStatus(
            Long paymentId,
            PaymentStatus newStatus
    );


    boolean isValidTransition(
            PaymentStatus oldStatus,
            PaymentStatus newStatus
    );


}