package com.citywater.burst.repo;

import com.citywater.burst.model.RoadRestoration;
import com.citywater.burst.model.RoadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoadRestorationRepo extends JpaRepository<RoadRestoration, Long> {
    Optional<RoadRestoration> findByOrderId(Long orderId);
    List<RoadRestoration> findAllByOrderByCreatedAtDesc();
    long countByStatusNot(RoadStatus status);
}
