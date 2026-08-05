package com.rupeex.main.repository;

import com.rupeex.main.entity.DeadLetterQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueueEntry, Long> {
    Optional<DeadLetterQueueEntry> findByPaymentId(Long paymentId);
}
