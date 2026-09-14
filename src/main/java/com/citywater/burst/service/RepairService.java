package com.citywater.burst.service;

import com.citywater.burst.config.CurrentUser;
import com.citywater.burst.dto.Requests.DispatchReq;
import com.citywater.burst.dto.Requests.ProgressReq;
import com.citywater.burst.dto.Requests.RestoreCheckReq;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 抢修调度：派单（同步阀门、许可、交通、备件、预计停水时间、送水点与协同方）、
 * 现场进度推进、复供前四项确认、复供放行。
 */
@Service
@RequiredArgsConstructor
public class RepairService {

    private final BurstEventRepo eventRepo;
    private final RepairOrderRepo orderRepo;
    private final ProgressLogRepo logRepo;
    private final RestorationCheckRepo checkRepo;
    private final ImpactAssessmentRepo assessmentRepo;
    private final NotifyService notifyService;
    private final CurrentUser currentUser;

    public List<RepairOrder> listOrders() {
        return orderRepo.findAllByOrderByCreatedAtDesc();
    }

    public RepairOrder getOrder(long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "工单不存在: " + id));
    }

    /**
     * 调度派单：把抢修队、客服、街道、物业、供水车、水质检测放在同一事件里推进。
     */
    @Transactional
    public RepairOrder dispatch(long eventId, DispatchReq req) {
        BurstEvent e = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "事件不存在: " + eventId));
        if (e.getStatus() != EventStatus.ASSESSED && e.getStatus() != EventStatus.DISPATCHED) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "请先完成影响评估再派单，当前状态: " + e.getStatus().getLabel());
        }
        ImpactAssessment a = assessmentRepo.findByEventId(eventId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "缺少影响评估，无法派单"));

        // 派单资料门禁：开挖许可、备件库存、应急送水点缺一不可，缺失时事件保持“已评估”
        List<String> missing = new ArrayList<>();
        if (isBlank(req.excavationPermitNo())) missing.add("开挖许可");
        if (isBlank(req.spareParts())) missing.add("备件库存");
        if (isBlank(req.waterPoints())) missing.add("应急送水点");
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "派单资料不完整，缺少: " + String.join("、", missing));
        }

        RepairOrder o = new RepairOrder();
        o.setOrderNo("R" + e.getEventNo().substring(1) + "-" + (orderRepo.findByEventIdOrderByCreatedAtDesc(eventId).size() + 1));
        o.setEvent(e);
        o.setTeamName(req.teamName());
        o.setCrewLeader(req.crewLeader());
        o.setCrewPhone(req.crewPhone());
        // 阀门位置默认采用评估生成的关阀方案
        o.setValveOps(isBlank(req.valveOps()) ? a.getValvePlan() : req.valveOps());
        o.setExcavationPermitNo(req.excavationPermitNo());
        o.setTrafficControl(req.trafficControl() != null ? req.trafficControl() : Boolean.TRUE.equals(a.getRoadAffected()));
        o.setTrafficPlan(req.trafficPlan());
        o.setSpareParts(req.spareParts());
        o.setEstimatedRestoreTime(LocalDateTime.now().plusHours(req.estimatedRestoreHours()));
        o.setWaterPoints(req.waterPoints());
        // 协同方：调度员指定与评估建议取并集
        o.setInvolveCs(req.involveCs() != null ? req.involveCs() : true);
        o.setInvolveStreet(bool(req.involveStreet()) || Boolean.TRUE.equals(a.getRestaurantAffected()) || Boolean.TRUE.equals(a.getRoadAffected()));
        o.setInvolveProperty(bool(req.involveProperty()) || Boolean.TRUE.equals(a.getHighRiseAffected()));
        o.setInvolveWaterTruck(bool(req.involveWaterTruck()) || Boolean.TRUE.equals(a.getHighRiseAffected())
                || Boolean.TRUE.equals(a.getHospitalAffected()) || a.getEstimatedOutageHours() >= 6);
        o.setInvolveQuality(bool(req.involveQuality()) || Boolean.TRUE.equals(a.getHospitalAffected()) || Boolean.TRUE.equals(a.getSchoolAffected()));
        o.setStage(RepairStage.DISPATCHED);
        o.setCreatedBy(currentUser.displayName());
        RepairOrder saved = orderRepo.save(o);

        e.setStatus(EventStatus.DISPATCHED);
        eventRepo.save(e);

        // 派单即生成首条停水通知
        notifyService.autoNotify(e, saved, RepairStage.DISPATCHED, currentUser.displayName());
        return saved;
    }

    /**
     * 现场进度上报：阶段严格按顺序逐段推进（到场→关阀→开挖→换管→冲洗→消毒→
     * 压力测试→道路恢复），保证事件时间线保留全部现场阶段；
     * 每次推进自动刷新客服通知内容。
     */
    @Transactional
    public ProgressLog addProgress(long orderId, ProgressReq req) {
        RepairOrder o = getOrder(orderId);
        RepairStage stage = req.stage();
        if (stage == RepairStage.COMPLETED) {
            throw new ResponseStatusException(BAD_REQUEST, "工单完成须通过复供确认，不能直接上报");
        }
        if (o.getStage() == RepairStage.COMPLETED) {
            throw new ResponseStatusException(BAD_REQUEST, "工单已完成，不能再上报现场进度");
        }
        if (o.getStage() == RepairStage.ROAD_RESTORED) {
            throw new ResponseStatusException(BAD_REQUEST, "现场阶段已全部记录完毕，请提交复供确认");
        }
        RepairStage next = nextStage(o.getStage());
        if (next == null || stage.getOrder() != next.getOrder()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "现场阶段须按顺序逐段推进，下一阶段应为: " + (next != null ? next.getLabel() : "无"));
        }
        ProgressLog log = new ProgressLog();
        log.setOrder(o);
        log.setStage(stage);
        log.setNote(req.note());
        log.setOperatorName(currentUser.displayName());
        ProgressLog saved = logRepo.save(log);

        o.setStage(stage);
        orderRepo.save(o);

        BurstEvent e = o.getEvent();
        if (stage.getOrder() >= RepairStage.ROAD_RESTORED.getOrder()) {
            e.setStatus(EventStatus.RESTORE_CHECK);
        } else {
            e.setStatus(EventStatus.REPAIRING);
        }
        eventRepo.save(e);

        // 通知内容随抢修进展变化
        notifyService.autoNotify(e, o, stage, currentUser.displayName());
        return saved;
    }

    /**
     * 复供前确认单：水压、水质、管网冲洗、用户通知四项逐项登记。
     */
    @Transactional
    public RestorationCheck upsertCheck(long orderId, RestoreCheckReq req) {
        RepairOrder o = getOrder(orderId);
        RestorationCheck c = checkRepo.findByOrderId(orderId).orElseGet(() -> {
            RestorationCheck nc = new RestorationCheck();
            nc.setOrder(o);
            return nc;
        });
        if (req.pressureOk() != null) c.setPressureOk(req.pressureOk());
        if (req.qualityOk() != null) c.setQualityOk(req.qualityOk());
        if (req.flushingOk() != null) c.setFlushingOk(req.flushingOk());
        if (req.notificationOk() != null) c.setNotificationOk(req.notificationOk());
        if (req.turbidity() != null) c.setTurbidity(req.turbidity());
        if (req.residualChlorine() != null) c.setResidualChlorine(req.residualChlorine());
        if (req.note() != null) c.setNote(req.note());
        if (req.qualityOk() != null || req.turbidity() != null || req.residualChlorine() != null) {
            c.setQualityInspector(currentUser.displayName());
        }
        RestorationCheck saved = checkRepo.save(c);

        BurstEvent e = o.getEvent();
        if (e.getStatus() == EventStatus.REPAIRING) {
            e.setStatus(EventStatus.RESTORE_CHECK);
            eventRepo.save(e);
        }
        return saved;
    }

    /**
     * 复供放行：必须完成道路恢复（全部现场阶段已按序记录），
     * 且水压、水质、管网冲洗、用户通知四项确认全部通过。
     */
    @Transactional
    public RepairOrder confirmRestore(long orderId) {
        RepairOrder o = getOrder(orderId);
        if (o.getStage().getOrder() < RepairStage.ROAD_RESTORED.getOrder()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "道路恢复未完成，不能复供，当前阶段: " + o.getStage().getLabel());
        }
        RestorationCheck c = checkRepo.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "尚未填写复供确认单"));
        List<String> missing = new ArrayList<>();
        if (!Boolean.TRUE.equals(c.getPressureOk())) missing.add("水压测试");
        if (!Boolean.TRUE.equals(c.getQualityOk())) missing.add("水质检测");
        if (!Boolean.TRUE.equals(c.getFlushingOk())) missing.add("管网冲洗");
        if (!Boolean.TRUE.equals(c.getNotificationOk())) missing.add("用户通知");
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "复供条件未满足: " + String.join("、", missing));
        }
        c.setConfirmedBy(currentUser.displayName());
        c.setConfirmedAt(LocalDateTime.now());
        checkRepo.save(c);

        o.setStage(RepairStage.COMPLETED);
        o.setCompletedAt(LocalDateTime.now());
        RepairOrder saved = orderRepo.save(o);

        BurstEvent e = o.getEvent();
        e.setStatus(EventStatus.RESTORED);
        e.setRestoredAt(LocalDateTime.now());
        eventRepo.save(e);

        notifyService.autoNotify(e, saved, RepairStage.COMPLETED, currentUser.displayName());
        return saved;
    }

    private RepairStage nextStage(RepairStage current) {
        for (RepairStage s : RepairStage.values()) {
            if (s.getOrder() == current.getOrder() + 1) {
                return s;
            }
        }
        return null;
    }

    private boolean bool(Boolean b) {
        return Boolean.TRUE.equals(b);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
