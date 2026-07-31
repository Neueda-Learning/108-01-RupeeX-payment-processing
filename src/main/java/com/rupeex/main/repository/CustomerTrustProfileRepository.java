package com.rupeex.main.repository;

import com.rupeex.main.entity.CustomerTrustProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerTrustProfileRepository extends JpaRepository<CustomerTrustProfile, Long> {

    Optional<CustomerTrustProfile> findByCustomerId(String customerId);
}

