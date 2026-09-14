package com.citywater.burst.repo;

import com.citywater.burst.model.MerchantLoss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchantLossRepo extends JpaRepository<MerchantLoss, Long> {
    List<MerchantLoss> findByEventId(Long eventId);
}
