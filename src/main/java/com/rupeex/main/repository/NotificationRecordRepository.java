package com.rupeex.main.repository;

import com.rupeex.main.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {
}
