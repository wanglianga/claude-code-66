package com.citywater.burst.repo;

import com.citywater.burst.model.ImpactAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImpactAssessmentRepo extends JpaRepository<ImpactAssessment, Long> {
    Optional<ImpactAssessment> findByEventId(Long eventId);
}
