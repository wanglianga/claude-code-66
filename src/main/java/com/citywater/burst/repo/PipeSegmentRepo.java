package com.citywater.burst.repo;

import com.citywater.burst.model.PipeSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PipeSegmentRepo extends JpaRepository<PipeSegment, Long> {
    List<PipeSegment> findByZoneId(Long zoneId);
}
