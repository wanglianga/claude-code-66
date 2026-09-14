package com.citywater.burst.service;

import com.citywater.burst.model.*;
import com.citywater.burst.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台看板：在办事件、长时停水预警、待处理问题、保障资源概览。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BurstEventRepo eventRepo;
    private final NotificationRepo notificationRepo;
    private final PostRestoreIssueRepo issueRepo;
    private final WaterPointRepo waterPointRepo;
    private final ElderlyDeliveryRepo elderlyRepo;
    private final MerchantLossRepo lossRepo;
    private final SecondaryTankRepo tankRepo;
    private final HospitalSupportRepo hospitalSupportRepo;
    private final YellowWaterCaseRepo yellowWaterCaseRepo;
    private final WaterQualityWatchRepo watchRepo;

    private static final List<EventStatus> ACTIVE = List.of(
            EventStatus.REPORTED, EventStatus.ASSESSED, EventStatus.DISPATCHED,
            EventStatus.REPAIRING, EventStatus.RESTORE_CHECK);

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalEvents", eventRepo.count());
        m.put("activeEvents", eventRepo.countByStatusIn(ACTIVE));
        m.put("restoredEvents", eventRepo.countByStatus(EventStatus.RESTORED));
        m.put("closedEvents", eventRepo.countByStatus(EventStatus.CLOSED));
        m.put("openIssues", issueRepo.countByStatusNot(IssueStatus.RESOLVED));

        List<BurstEvent> active = eventRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(e -> ACTIVE.contains(e.getStatus()))
                .toList();
        m.put("activeList", active);

        // 长时停水预警：在办且超过 6 小时未复供
        List<BurstEvent> longOutage = active.stream()
                .filter(e -> e.getCreatedAt().isBefore(LocalDateTime.now().minusHours(6)))
                .toList();
        m.put("longOutageEvents", longOutage);

        m.put("recentNotifications", notificationRepo.findTop20ByOrderByCreatedAtDesc());
        m.put("openIssueList", issueRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(i -> i.getStatus() != IssueStatus.RESOLVED)
                .limit(10)
                .toList());

        Map<String, Object> support = new LinkedHashMap<>();
        support.put("openWaterPoints", waterPointRepo.findAll().stream()
                .filter(p -> p.getStatus() != WaterPointStatus.CLOSED).count());
        support.put("pendingDeliveries", elderlyRepo.findAll().stream()
                .filter(d -> d.getStatus() != AidStatus.DONE).count());
        support.put("unsettledLosses", lossRepo.findAll().stream()
                .filter(l -> l.getStatus() != LossStatus.SETTLED).count());
        support.put("abnormalTanks", tankRepo.findAll().stream()
                .filter(t -> t.getStatus() == TankStatus.LOW || t.getStatus() == TankStatus.EMPTY).count());
        m.put("support", support);

        // 医院应急供水保障：待确认数量与清单（客服端可见，避免重复催问抢修队）
        List<HospitalSupport> hospitalSupports = hospitalSupportRepo.findAllByOrderByCreatedAtDesc();
        m.put("hospitalSupportActive",
                hospitalSupports.stream().filter(s -> s.getStatus() != HospitalSupportStatus.CONFIRMED).count());
        m.put("hospitalSupports", hospitalSupports);

        // 黄水投诉与重点水质观察
        m.put("yellowWaterOpen", yellowWaterCaseRepo.countByStatusNot(YwStatus.DONE));
        m.put("watchCommunities", watchRepo.countByStatus(WatchStatus.WATCHING));
        m.put("watchList", watchRepo.findAllByOrderByUpdatedAtDesc().stream()
                .filter(w -> w.getStatus() == WatchStatus.WATCHING).toList());
        return m;
    }
}
