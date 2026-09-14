package com.citywater.burst.repo;

import com.citywater.burst.model.YellowWaterCase;
import com.citywater.burst.model.YwStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YellowWaterCaseRepo extends JpaRepository<YellowWaterCase, Long> {
    List<YellowWaterCase> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<YellowWaterCase> findByCommunityOrderByCreatedAtDesc(String community);
    List<YellowWaterCase> findAllByOrderByCreatedAtDesc();
    long countByCommunity(String community);
    long countByCommunityAndHighRiseTrue(String community);
    long countByStatusNot(YwStatus status);
}
