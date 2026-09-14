package com.citywater.burst.repo;

import com.citywater.burst.model.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityRepo extends JpaRepository<Facility, Long> {
    List<Facility> findByZoneId(Long zoneId);
}
