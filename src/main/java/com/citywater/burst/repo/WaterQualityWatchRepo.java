package com.citywater.burst.repo;

import com.citywater.burst.model.WaterQualityWatch;
import com.citywater.burst.model.WatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaterQualityWatchRepo extends JpaRepository<WaterQualityWatch, Long> {
    Optional<WaterQualityWatch> findByCommunity(String community);
    List<WaterQualityWatch> findAllByOrderByUpdatedAtDesc();
    long countByStatus(WatchStatus status);
}
