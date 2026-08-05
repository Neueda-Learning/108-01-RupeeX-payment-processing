package com.rupeex.main.repository;

import com.rupeex.main.entity.SystemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SystemEventRepository extends JpaRepository<SystemEvent, Long> {
    List<SystemEvent> findTop100ByOrderByCreatedAtDesc();
}
