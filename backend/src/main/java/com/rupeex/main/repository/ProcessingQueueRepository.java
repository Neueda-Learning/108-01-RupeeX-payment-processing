package com.rupeex.main.repository;

import com.rupeex.main.entity.ProcessingQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProcessingQueueRepository extends JpaRepository<ProcessingQueueEntry, Long> {
    Optional<ProcessingQueueEntry> findByPaymentId(Long paymentId);

    @Query("select q from ProcessingQueueEntry q where q.status = :status and q.nextAttemptAt <= :now")
    List<ProcessingQueueEntry> findReadyEntries(@Param("status") String status, @Param("now") LocalDateTime now);

    long countByStatus(String status);
}
