package com.citywater.burst.repo;

import com.citywater.burst.model.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairOrderRepo extends JpaRepository<RepairOrder, Long> {
    List<RepairOrder> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<RepairOrder> findAllByOrderByCreatedAtDesc();
}
