package com.rupeex.main.repository;

import com.rupeex.main.entity.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {
    Optional<RiskScore> findByPaymentId(Long paymentId);
}
