package com.rupeex.main.service;

import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.enums.PaymentStatus;

import java.util.List;

public interface PaymentAuditService {


    void createHistory(
            Long paymentId,
            PaymentStatus oldStatus,
            PaymentStatus newStatus,
            String reason
    );


    List<PaymentHistory>
    getHistory(Long paymentId);


}