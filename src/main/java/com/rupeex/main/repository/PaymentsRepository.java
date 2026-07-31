package com.rupeex.main.repository;

import com.rupeex.main.model.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, Long> {
    Optional<Payments> findByIdempotencyKey(String idempotencyKey);
}

