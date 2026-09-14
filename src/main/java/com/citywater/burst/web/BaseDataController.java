package com.citywater.burst.web;

import com.citywater.burst.model.*;
import com.citywater.burst.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础资料：阀门分区、管段（历史管线）、阀门台账、重点设施。
 */
@RestController
@RequestMapping("/api/base")
@RequiredArgsConstructor
public class BaseDataController {

    private final ValveZoneRepo zoneRepo;
    private final PipeSegmentRepo pipeRepo;
    private final ValveRepo valveRepo;
    private final FacilityRepo facilityRepo;

    @GetMapping("/zones")
    public List<ValveZone> zones() {
        return zoneRepo.findAll();
    }

    @GetMapping("/pipes")
    public List<PipeSegment> pipes() {
        return pipeRepo.findAll();
    }

    @GetMapping("/valves")
    public List<Valve> valves() {
        return valveRepo.findAll();
    }

    @GetMapping("/facilities")
    public List<Facility> facilities() {
        return facilityRepo.findAll();
    }

    /** 下拉框用的枚举与中文标签 */
    @GetMapping("/enums")
    public Map<String, Map<String, String>> enums() {
        Map<String, Map<String, String>> m = new LinkedHashMap<>();
        m.put("eventSource", labels(EventSource.values()));
        m.put("eventStatus", labels(EventStatus.values()));
        m.put("severity", labels(Severity.values()));
        m.put("repairStage", labels(RepairStage.values()));
        m.put("notifyChannel", labels(NotifyChannel.values()));
        m.put("issueType", labels(IssueType.values()));
        m.put("issueStatus", labels(IssueStatus.values()));
        m.put("aidStatus", labels(AidStatus.values()));
        m.put("lossStatus", labels(LossStatus.values()));
        m.put("tankStatus", labels(TankStatus.values()));
        m.put("waterPointStatus", labels(WaterPointStatus.values()));
        m.put("hospitalSupportStatus", labels(HospitalSupportStatus.values()));
        m.put("facilityType", labels(FacilityType.values()));
        return m;
    }

    private static <E extends Enum<E>> Map<String, String> labels(E[] values) {
        Map<String, String> m = new LinkedHashMap<>();
        for (E v : values) {
            try {
                m.put(v.name(), (String) v.getClass().getMethod("getLabel").invoke(v));
            } catch (Exception ex) {
                m.put(v.name(), v.name());
            }
        }
        return m;
    }
}
