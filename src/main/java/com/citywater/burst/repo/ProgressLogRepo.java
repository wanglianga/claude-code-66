package com.citywater.burst.repo;

import com.citywater.burst.model.ProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressLogRepo extends JpaRepository<ProgressLog, Long> {
    List<ProgressLog> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
