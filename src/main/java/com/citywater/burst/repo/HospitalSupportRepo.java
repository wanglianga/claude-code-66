package com.citywater.burst.repo;

import com.citywater.burst.model.HospitalSupport;
import com.citywater.burst.model.HospitalSupportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalSupportRepo extends JpaRepository<HospitalSupport, Long> {
    List<HospitalSupport> findByEventId(Long eventId);
    List<HospitalSupport> findAllByOrderByCreatedAtDesc();
    boolean existsByEventIdAndHospitalName(Long eventId, String hospitalName);
    long countByStatusNot(HospitalSupportStatus status);
}
