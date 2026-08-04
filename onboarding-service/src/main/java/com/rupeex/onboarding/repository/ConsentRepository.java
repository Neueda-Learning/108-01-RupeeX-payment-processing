package com.rupeex.onboarding.repository;

import com.rupeex.onboarding.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, Long> {

    boolean existsByCustomer_IdAndAcceptedTrue(UUID customerId);
}
