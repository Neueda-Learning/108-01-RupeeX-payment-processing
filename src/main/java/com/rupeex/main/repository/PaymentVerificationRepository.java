package com.rupeex.main.repository;

import com.rupeex.main.entity.PaymentVerification;
import com.rupeex.main.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentVerificationRepository extends JpaRepository<PaymentVerification, Long> {

    Optional<PaymentVerification> findByPaymentId(Long paymentId);

    Optional<PaymentVerification> findByPaymentIdAndVerificationTokenAndStatus(
            Long paymentId,
            String verificationToken,
            VerificationStatus status
    );
}

