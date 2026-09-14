package com.citywater.burst.repo;

import com.citywater.burst.model.Valve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValveRepo extends JpaRepository<Valve, Long> {
    List<Valve> findByZoneId(Long zoneId);
}
