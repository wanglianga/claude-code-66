package com.citywater.burst.repo;

import com.citywater.burst.model.ElderlyDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElderlyDeliveryRepo extends JpaRepository<ElderlyDelivery, Long> {
    List<ElderlyDelivery> findByEventId(Long eventId);
}
