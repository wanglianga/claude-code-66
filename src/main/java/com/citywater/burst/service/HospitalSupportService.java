package com.citywater.burst.service;

import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.BurstEventRepo;
import com.citywater.burst.repo.HospitalSupportRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 医院应急供水保障：影响医院时优先识别透析/手术/消毒供应/住院楼需求，
 * 调度供水车与临时水箱，对接医院后勤；供水到位、用水量、复供时间写回抢修事件；
 * 供水车无法进院时记录改设水点与志愿者送水；医院确认后客服端可见保障完成。
 */
@Service
@RequiredArgsConstructor
public class HospitalSupportService {

    private final HospitalSupportRepo supportRepo;
    private final BurstEventRepo eventRepo;

    public List<HospitalSupport> list(Long eventId) {
        return eventId == null
                ? supportRepo.findAllByOrderByCreatedAtDesc()
                : supportRepo.findByEventId(eventId);
    }

    public HospitalSupport get(long id) {
        return supportRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "医疗保障单不存在: " + id));
    }

    /** 手动登记（评估未覆盖到的医院） */
    @Transactional
    public HospitalSupport create(HospitalSupportCreateReq req) {
        BurstEvent e = eventRepo.findById(req.eventId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "事件不存在: " + req.eventId()));
        if (supportRepo.existsByEventIdAndHospitalName(e.getId(), req.hospitalName())) {
            throw new ResponseStatusException(BAD_REQUEST, "该事件已存在 " + req.hospitalName() + " 的保障单");
        }
        HospitalSupport s = new HospitalSupport();
        s.setEvent(e);
        s.setHospitalName(req.hospitalName());
        if (req.needDialysis() != null) s.setNeedDialysis(req.needDialysis());
        if (req.needSurgery() != null) s.setNeedSurgery(req.needSurgery());
        if (req.needSterileSupply() != null) s.setNeedSterileSupply(req.needSterileSupply());
        if (req.needInpatient() != null) s.setNeedInpatient(req.needInpatient());
        s.setLogisticsContact(req.logisticsContact());
        s.setLogisticsPhone(req.logisticsPhone());
        return supportRepo.save(s);
    }

    /**
     * 派单时自动为受影响医院创建保障单：优先识别透析、手术、消毒供应、住院楼需求，
     * 并带入医院后勤联系人。
     */
    @Transactional
    public void autoCreateFor(BurstEvent e, ImpactAssessment a) {
        if (a == null || a.getAffectedFacilities() == null) {
            return;
        }
        for (Facility f : a.getAffectedFacilities()) {
            if (f.getType() != FacilityType.HOSPITAL) {
                continue;
            }
            if (supportRepo.existsByEventIdAndHospitalName(e.getId(), f.getName())) {
                continue;
            }
            HospitalSupport s = new HospitalSupport();
            s.setEvent(e);
            s.setHospitalName(f.getName());
            s.setLogisticsContact(f.getContactName());
            s.setLogisticsPhone(f.getContactPhone());
            supportRepo.save(s);
        }
    }

    /** 调度供水车与临时水箱 */
    @Transactional
    public HospitalSupport dispatchSupply(long id, HospitalDispatchReq req) {
        HospitalSupport s = get(id);
        if (s.getStatus() != HospitalSupportStatus.PENDING && s.getStatus() != HospitalSupportStatus.DELIVERING) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "当前状态不能调度供水车: " + s.getStatus().getLabel());
        }
        s.setWaterTrucks(req.waterTrucks());
        if (req.tempTanks() != null) {
            s.setTempTanks(req.tempTanks());
        }
        s.setStatus(HospitalSupportStatus.DELIVERING);
        return supportRepo.save(s);
    }

    /** 供水到位：记录到位时间与用水量（写入抢修事件） */
    @Transactional
    public HospitalSupport markSupplied(long id, HospitalSupplyReq req) {
        HospitalSupport s = get(id);
        if (s.getStatus() != HospitalSupportStatus.DELIVERING) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "请先调度供水车，再登记供水到位，当前状态: " + s.getStatus().getLabel());
        }
        s.setArrivedAt(LocalDateTime.now());
        s.setWaterAmountM3(req.waterAmountM3());
        if (req.restoreTime() != null) {
            s.setRestoreTime(req.restoreTime());
        }
        s.setStatus(HospitalSupportStatus.SUPPLIED);
        return supportRepo.save(s);
    }

    /** 供水车无法进入院区：记录改设水点与志愿者送水 */
    @Transactional
    public HospitalSupport recordAccessIssue(long id, AccessIssueReq req) {
        HospitalSupport s = get(id);
        if (s.getStatus() == HospitalSupportStatus.CONFIRMED) {
            throw new ResponseStatusException(BAD_REQUEST, "保障已由医院确认完成，不能再登记通行问题");
        }
        s.setTruckAccessIssue(true);
        s.setAltWaterPoint(req.altWaterPoint());
        if (req.volunteers() != null) {
            s.setVolunteers(req.volunteers());
        }
        return supportRepo.save(s);
    }

    /** 医院确认：确认后客服端显示医疗用水保障完成 */
    @Transactional
    public HospitalSupport confirm(long id, HospitalConfirmReq req) {
        HospitalSupport s = get(id);
        if (s.getStatus() != HospitalSupportStatus.SUPPLIED) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "供水到位后才能由医院确认，当前状态: " + s.getStatus().getLabel());
        }
        s.setHospitalConfirmer(req.hospitalConfirmer());
        s.setConfirmedAt(LocalDateTime.now());
        s.setStatus(HospitalSupportStatus.CONFIRMED);
        return supportRepo.save(s);
    }

    /** 复盘记录：保障过程纳入复盘 */
    @Transactional
    public HospitalSupport addReview(long id, ReviewReq req) {
        HospitalSupport s = get(id);
        s.setReviewNote(req.reviewNote());
        return supportRepo.save(s);
    }

    /** 事件复供时，把复供时间写入各保障单（随事件归档） */
    @Transactional
    public void syncRestoreTime(BurstEvent e) {
        for (HospitalSupport s : supportRepo.findByEventId(e.getId())) {
            if (s.getRestoreTime() == null) {
                s.setRestoreTime(e.getRestoredAt());
                supportRepo.save(s);
            }
        }
    }
}
