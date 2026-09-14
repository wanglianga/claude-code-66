package com.citywater.burst.repo;

import com.citywater.burst.model.BurstEvent;
import com.citywater.burst.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BurstEventRepo extends JpaRepository<BurstEvent, Long> {
    List<BurstEvent> findAllByOrderByCreatedAtDesc();
    List<BurstEvent> findByStatusOrderByCreatedAtDesc(EventStatus status);
    long countByStatus(EventStatus status);
    long countByStatusIn(List<EventStatus> statuses);
}
