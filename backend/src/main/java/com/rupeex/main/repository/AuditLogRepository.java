package com.rupeex.main.repository;

import com.rupeex.main.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
