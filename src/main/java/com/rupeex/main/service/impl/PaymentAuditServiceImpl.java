package com.rupeex.main.service.impl;

import com.rupeex.main.repository.PaymentHistoryRepository;

@Service
public class PaymentAuditServiceImpl
        implements PaymentAuditService{


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


}