package com.rupeex.main.repository;


import com.rupeex.main.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PaymentHistoryRepository
        extends JpaRepository<PaymentHistory, Long> {


    // Get complete history of a payment ordered by newest first
    List<PaymentHistory> findByPaymentIdOrderByChangedAtDesc(Long paymentId);


}