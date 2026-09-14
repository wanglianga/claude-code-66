package com.citywater.burst.repo;

import com.citywater.burst.model.RestorationCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestorationCheckRepo extends JpaRepository<RestorationCheck, Long> {
    Optional<RestorationCheck> findByOrderId(Long orderId);
}
