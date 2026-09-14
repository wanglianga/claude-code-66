package com.citywater.burst.repo;

import com.citywater.burst.model.WaterPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaterPointRepo extends JpaRepository<WaterPoint, Long> {
    List<WaterPoint> findByEventId(Long eventId);
}
