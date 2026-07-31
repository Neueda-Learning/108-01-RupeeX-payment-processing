package com.rupeex.main.service.impl;

import com.rupeex.main.entity.PaymentHistory;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.repository.PaymentHistoryRepository;
import com.rupeex.main.service.PaymentAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentAuditServiceImpl
        implements PaymentAuditService {


    @Autowired
    private PaymentHistoryRepository repository;



    @Override
    public void createHistory(
            Long paymentId,
            PaymentStatus oldStatus,
            PaymentStatus newStatus,
            String reason){


        PaymentHistory history =
                new PaymentHistory();


        history.setPaymentId(paymentId);

        history.setOldStatus(oldStatus);

        history.setNewStatus(newStatus);

        history.setReason(reason);


        repository.save(history);

    }

    @Override
    public List<PaymentHistory> getHistory(Long paymentId) {
        return List.of();
    }


}