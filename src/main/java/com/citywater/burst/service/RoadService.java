package com.citywater.burst.service;

import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.RoadRestorationRepo;
import com.citywater.burst.repo.SubsidenceReportRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 道路恢复验收：回填/围挡撤除/交通恢复/路面照片由现场负责人、城管或道路单位确认；
 * 验收不通过时围挡和交通提示继续保持；沉降责任追溯施工班组、材料批次、恢复时间；
 * 沉降复查安排固定日期。
 */
@Service
@RequiredArgsConstructor
public class RoadService {

    private final RoadRestorationRepo roadRepo;
    private final SubsidenceReportRepo subsidenceRepo;

    public List<RoadRestoration> list() {
        return roadRepo.findAllByOrderByCreatedAtDesc();
    }

    public RoadRestoration get(long id) {
        return roadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "道路恢复验收单不存在: " + id));
    }

    /** 工单进入道路恢复阶段时自动创建验收单 */
    @Transactional
    public void createForOrder(RepairOrder o) {
        if (roadRepo.findByOrderId(o.getId()).isPresent()) {
            return;
        }
        RoadRestoration r = new RoadRestoration();
        r.setOrder(o);
        r.setConstructionCrew(o.getTeamName());
        r.setRestoredAt(LocalDateTime.now());
        roadRepo.save(r);
    }

    /** 提交验收材料：回填、围挡撤除、交通恢复、路面照片、施工班组、材料批次 */
    @Transactional
    public RoadRestoration submit(long id, RoadSubmitReq req) {
        RoadRestoration r = get(id);
        if (r.getStatus() != RoadStatus.PENDING && r.getStatus() != RoadStatus.REJECTED) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "当前状态不能提交验收: " + r.getStatus().getLabel());
        }
        if (!Boolean.TRUE.equals(req.backfillDone()) || !Boolean.TRUE.equals(req.barrierRemoved())
                || !Boolean.TRUE.equals(req.trafficRestored())) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "验收材料不齐全：道路回填、围挡撤除、交通恢复须全部完成");
        }
        r.setBackfillDone(true);
        r.setBarrierRemoved(true);
        r.setTrafficRestored(true);
        r.setPhotoUrls(req.photoUrls());
        r.setConstructionCrew(req.constructionCrew());
        r.setMaterialBatch(req.materialBatch());
        r.setStatus(RoadStatus.SUBMITTED);
        r.setRejectReason(null);
        return roadRepo.save(r);
    }

    /** 验收通过（现场负责人/城管/道路单位确认） */
    @Transactional
    public RoadRestoration accept(long id, RoadAcceptReq req) {
        RoadRestoration r = get(id);
        if (r.getStatus() != RoadStatus.SUBMITTED) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "仅待确认状态可验收通过，当前状态: " + r.getStatus().getLabel());
        }
        r.setConfirmer(req.confirmer());
        r.setConfirmerRole(req.confirmerRole());
        r.setConfirmedAt(LocalDateTime.now());
        r.setStatus(RoadStatus.ACCEPTED);
        r.setBarrierMaintained(false);
        r.setRejectReason(null);
        return roadRepo.save(r);
    }

    /** 验收不通过：围挡和交通提示继续保持 */
    @Transactional
    public RoadRestoration reject(long id, RoadRejectReq req) {
        RoadRestoration r = get(id);
        if (r.getStatus() != RoadStatus.SUBMITTED) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "仅待确认状态可验收不通过，当前状态: " + r.getStatus().getLabel());
        }
        r.setConfirmer(req.confirmer());
        r.setConfirmerRole(req.confirmerRole());
        r.setConfirmedAt(LocalDateTime.now());
        r.setStatus(RoadStatus.REJECTED);
        r.setRejectReason(req.reason());
        r.setBarrierMaintained(true);
        return roadRepo.save(r);
    }

    // ---- 沉降记录 ----

    public List<SubsidenceReport> subsidenceList() {
        return subsidenceRepo.findAllByOrderByCreatedAtDesc();
    }

    /** 登记沉降（手动或投诉自动关联）：复查安排固定日期 */
    @Transactional
    public SubsidenceReport addSubsidence(long roadId, String description, java.time.LocalDate recheckDate,
                                          PostRestoreIssue issue) {
        RoadRestoration r = get(roadId);
        SubsidenceReport s = new SubsidenceReport();
        s.setRoadRestoration(r);
        s.setIssue(issue);
        s.setDescription(description);
        s.setRecheckDate(recheckDate);
        return subsidenceRepo.save(s);
    }

    /** 沉降复查：记录复查结果 */
    @Transactional
    public SubsidenceReport recheck(long subsidenceId, RecheckReq req) {
        SubsidenceReport s = subsidenceRepo.findById(subsidenceId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "沉降记录不存在: " + subsidenceId));
        if (s.getStatus() == SubsidenceStatus.RECHECKED) {
            throw new ResponseStatusException(BAD_REQUEST, "该沉降记录已复查");
        }
        s.setRecheckResult(req.result());
        s.setRecheckedAt(LocalDateTime.now());
        s.setStatus(SubsidenceStatus.RECHECKED);
        return subsidenceRepo.save(s);
    }
}
