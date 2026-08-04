package com.rupeex.main.repository;

import com.rupeex.main.entity.FraudResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FraudResultRepository extends JpaRepository<FraudResult, Long> {
    List<FraudResult> findByPaymentId(Long paymentId);
}
