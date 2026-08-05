package com.rupeex.main.repository;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {


    // Find payment using idempotency key
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);


    // Find payments by status
    List<Payment> findByStatus(PaymentStatus status);


    // Search payment using reference
    Optional<Payment> findByPaymentReference(String paymentReference);


    // Check whether duplicate payment exists
    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findTopBySourceAccountOrderByCreatedAtDesc(String sourceAccount);

    long countBySourceAccountAndCreatedAtAfter(
            String sourceAccount,
            LocalDateTime createdAt
    );

    @Query("select avg(p.amount) from Payment p where p.sourceAccount = :sourceAccount")
    Double findAverageAmountBySourceAccount(
            @Param("sourceAccount") String sourceAccount
    );

    long countBySourceAccountAndStatus(String sourceAccount, PaymentStatus status);
}