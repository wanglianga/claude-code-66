package com.citywater.burst.repo;

import com.citywater.burst.model.SecondaryTank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecondaryTankRepo extends JpaRepository<SecondaryTank, Long> {
    List<SecondaryTank> findByEventId(Long eventId);
}
