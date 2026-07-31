package com.rupeex.main.repository;

import com.rupeex.main.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

}