package com.citywater.burst.repo;

import com.citywater.burst.model.SubsidenceReport;
import com.citywater.burst.model.SubsidenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubsidenceReportRepo extends JpaRepository<SubsidenceReport, Long> {
    List<SubsidenceReport> findByRoadRestorationId(Long roadRestorationId);
    List<SubsidenceReport> findAllByOrderByCreatedAtDesc();
    long countByStatus(SubsidenceStatus status);
}
