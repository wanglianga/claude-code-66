package com.citywater.burst.service;

import com.citywater.burst.config.CurrentUser;
import com.citywater.burst.dto.Requests.EventCreateReq;
import com.citywater.burst.dto.Requests.EventDetail;
import com.citywater.burst.dto.Requests.OrderDetail;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 爆管事件：报警上报、影响评估、事件详情。
 */
@Service
@RequiredArgsConstructor
public class BurstService {

    private final BurstEventRepo eventRepo;
    private final ValveZoneRepo zoneRepo;
    private final PipeSegmentRepo pipeRepo;
    private final ValveRepo valveRepo;
    private final FacilityRepo facilityRepo;
    private final ImpactAssessmentRepo assessmentRepo;
    private final RepairOrderRepo orderRepo;
    private final ProgressLogRepo logRepo;
    private final RestorationCheckRepo checkRepo;
    private final PostRestoreIssueRepo issueRepo;
    private final NotificationRepo notificationRepo;
    private final WaterPointRepo waterPointRepo;
    private final ElderlyDeliveryRepo elderlyRepo;
    private final MerchantLossRepo lossRepo;
    private final SecondaryTankRepo tankRepo;
    private final HospitalSupportRepo hospitalSupportRepo;
    private final CurrentUser currentUser;

    public List<BurstEvent> list(EventStatus status) {
        return status == null
                ? eventRepo.findAllByOrderByCreatedAtDesc()
                : eventRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    public BurstEvent get(long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "事件不存在: " + id));
    }

    @Transactional
    public BurstEvent create(EventCreateReq req) {
        ValveZone zone = zoneRepo.findById(req.zoneId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "分区不存在: " + req.zoneId()));
        BurstEvent e = new BurstEvent();
        e.setEventNo(nextEventNo());
        e.setSource(req.source());
        e.setLocation(req.location());
        e.setDescription(req.description());
        e.setZone(zone);
        if (req.pipeSegmentId() != null) {
            PipeSegment seg = pipeRepo.findById(req.pipeSegmentId())
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "管段不存在: " + req.pipeSegmentId()));
            e.setPipeSegment(seg);
        }
        e.setReporterName(req.reporterName());
        e.setReporterPhone(req.reporterPhone());
        e.setStatus(EventStatus.REPORTED);
        return eventRepo.save(e);
    }

    /**
     * 影响评估：根据管径、阀门分区、周边小区/医院/学校/餐饮街、
     * 道路交通和历史管线资料自动生成停水影响范围。
     */
    @Transactional
    public ImpactAssessment assess(long eventId) {
        BurstEvent e = get(eventId);
        if (e.getStatus() != EventStatus.REPORTED) {
            throw new ResponseStatusException(BAD_REQUEST, "仅“已报警”状态可评估，当前状态: " + e.getStatus().getLabel());
        }
        Long zoneId = e.getZone().getId();
        List<Facility> facilities = facilityRepo.findByZoneId(zoneId);
        List<Valve> valves = valveRepo.findByZoneId(zoneId);

        ImpactAssessment a = new ImpactAssessment();
        a.setEvent(e);
        a.setAffectedFacilities(facilities);

        int population = facilities.stream()
                .filter(f -> f.getType() != FacilityType.ROAD)
                .mapToInt(f -> f.getPopulation() == null ? 0 : f.getPopulation())
                .sum();
        a.setAffectedPopulation(population);
        a.setAffectedHouseholds(population / 3);

        boolean hospital = hasType(facilities, FacilityType.HOSPITAL);
        boolean school = hasType(facilities, FacilityType.SCHOOL);
        boolean restaurant = hasType(facilities, FacilityType.RESTAURANT);
        boolean road = hasType(facilities, FacilityType.ROAD);
        boolean highRise = facilities.stream().anyMatch(f -> Boolean.TRUE.equals(f.getHighRise()));
        a.setHospitalAffected(hospital);
        a.setSchoolAffected(school);
        a.setRestaurantAffected(restaurant);
        a.setRoadAffected(road);
        a.setHighRiseAffected(highRise);

        // 关阀方案：分区阀门台账
        String valvePlan = valves.isEmpty()
                ? "分区 " + e.getZone().getCode() + " 无登记阀门，需现场核查"
                : "关闭 " + valves.stream()
                        .map(v -> v.getCode() + "（" + v.getLocation() + "）")
                        .collect(Collectors.joining("、"))
                        + "，隔离分区 " + e.getZone().getCode() + " " + e.getZone().getName();
        a.setValvePlan(valvePlan);

        // 历史管线资料分析
        PipeSegment seg = e.getPipeSegment();
        if (seg != null) {
            int age = Year.now().getValue() - seg.getInstallYear();
            String risk = switch (seg.getMaterial()) {
                case "铸铁", "镀锌管" -> "材质老化，属高风险管段";
                case "钢管" -> "注意焊缝与腐蚀";
                default -> "材质风险较低";
            };
            a.setPipeAnalysis("管段 " + seg.getCode() + "：DN" + seg.getDiameterMm() + " " + seg.getMaterial()
                    + "，" + seg.getInstallYear() + "年敷设（管龄约" + age + "年），位于" + seg.getRoadName()
                    + "；" + risk);
        } else {
            a.setPipeAnalysis("未关联管段，需现场核实管线资料");
        }

        // 交通影响
        String roads = facilities.stream()
                .filter(f -> f.getType() == FacilityType.ROAD)
                .map(Facility::getName)
                .collect(Collectors.joining("、"));
        a.setTrafficImpact(road
                ? "开挖影响 " + roads + " 通行，需交通协管与绕行提示"
                : "对主干道无明显影响");

        // 建议级别与预计停水时长
        int diameter = seg != null ? seg.getDiameterMm() : 200;
        Severity severity;
        if (hospital || diameter >= 600) {
            severity = Severity.CRITICAL;
        } else if (school || restaurant || highRise || diameter >= 400) {
            severity = Severity.MAJOR;
        } else if (diameter >= 200) {
            severity = Severity.MODERATE;
        } else {
            severity = Severity.MINOR;
        }
        a.setSuggestedSeverity(severity);
        int hours = 3 + diameter / 200 + (highRise ? 1 : 0) + (hospital ? 1 : 0);
        a.setEstimatedOutageHours(hours);

        a.setAssessor(currentUser.displayName());
        ImpactAssessment saved = assessmentRepo.save(a);

        e.setSeverity(severity);
        e.setStatus(EventStatus.ASSESSED);
        eventRepo.save(e);
        return saved;
    }

    private boolean hasType(List<Facility> facilities, FacilityType type) {
        return facilities.stream().anyMatch(f -> f.getType() == type);
    }

    /**
     * 事件完整详情：评估、工单（进度/复供单/复供后问题）、通知、停水保障。
     */
    @Transactional(readOnly = true)
    public EventDetail detail(long eventId) {
        BurstEvent e = get(eventId);
        ImpactAssessment assessment = assessmentRepo.findByEventId(eventId).orElse(null);
        List<OrderDetail> orders = orderRepo.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(o -> new OrderDetail(
                        o,
                        logRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()),
                        checkRepo.findByOrderId(o.getId()).orElse(null),
                        issueRepo.findByOrderIdOrderByCreatedAtDesc(o.getId())))
                .toList();
        return new EventDetail(
                e,
                assessment,
                orders,
                notificationRepo.findByEventIdOrderByCreatedAtDesc(eventId),
                waterPointRepo.findByEventId(eventId),
                elderlyRepo.findByEventId(eventId),
                lossRepo.findByEventId(eventId),
                tankRepo.findByEventId(eventId),
                hospitalSupportRepo.findByEventId(eventId));
    }

    /** 关闭事件：必须已复供且复供后问题全部解决，形成完整闭环。 */
    @Transactional
    public BurstEvent close(long eventId) {
        BurstEvent e = get(eventId);
        if (e.getStatus() != EventStatus.RESTORED) {
            throw new ResponseStatusException(BAD_REQUEST, "仅“已复供”状态可关闭，当前状态: " + e.getStatus().getLabel());
        }
        List<RepairOrder> orders = orderRepo.findByEventIdOrderByCreatedAtDesc(eventId);
        long openIssues = orders.stream()
                .flatMap(o -> issueRepo.findByOrderIdOrderByCreatedAtDesc(o.getId()).stream())
                .filter(i -> i.getStatus() != IssueStatus.RESOLVED)
                .count();
        if (openIssues > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "仍有 " + openIssues + " 条复供后问题未解决，不能关闭事件");
        }
        e.setStatus(EventStatus.CLOSED);
        e.setClosedAt(LocalDateTime.now());
        return eventRepo.save(e);
    }

    private String nextEventNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "E" + date + "-" + String.format("%03d", eventRepo.count() + 1);
    }
}
