package com.citywater.burst.service;

import com.citywater.burst.config.CurrentUser;
import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 长时间停水保障：临时水点排队、老人送水、商户停业损失、二次供水水箱状态。
 */
@Service
@RequiredArgsConstructor
public class SupportService {

    private final BurstEventRepo eventRepo;
    private final WaterPointRepo waterPointRepo;
    private final ElderlyDeliveryRepo elderlyRepo;
    private final MerchantLossRepo lossRepo;
    private final SecondaryTankRepo tankRepo;
    private final CurrentUser currentUser;

    private BurstEvent event(long eventId) {
        return eventRepo.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "事件不存在: " + eventId));
    }

    // ---- 应急送水点 ----

    public List<WaterPoint> waterPoints(Long eventId) {
        return eventId == null ? waterPointRepo.findAll() : waterPointRepo.findByEventId(eventId);
    }

    @Transactional
    public WaterPoint addWaterPoint(WaterPointReq req) {
        WaterPoint p = new WaterPoint();
        p.setEvent(event(req.eventId()));
        p.setName(req.name());
        p.setLocation(req.location());
        p.setNote(req.note());
        return waterPointRepo.save(p);
    }

    @Transactional
    public WaterPoint updateWaterPoint(long id, WaterPointUpdateReq req) {
        WaterPoint p = waterPointRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "送水点不存在: " + id));
        if (req.queueLength() != null) p.setQueueLength(req.queueLength());
        if (req.status() != null) p.setStatus(req.status());
        if (req.note() != null) p.setNote(req.note());
        return waterPointRepo.save(p);
    }

    // ---- 老人送水 ----

    public List<ElderlyDelivery> elderly(Long eventId) {
        return eventId == null ? elderlyRepo.findAll() : elderlyRepo.findByEventId(eventId);
    }

    @Transactional
    public ElderlyDelivery addElderly(ElderlyReq req) {
        ElderlyDelivery d = new ElderlyDelivery();
        d.setEvent(event(req.eventId()));
        d.setElderName(req.elderName());
        d.setAddress(req.address());
        d.setPhone(req.phone());
        d.setDeliverer(req.deliverer());
        d.setNote(req.note());
        return elderlyRepo.save(d);
    }

    @Transactional
    public ElderlyDelivery updateElderly(long id, AidStatusReq req) {
        ElderlyDelivery d = elderlyRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "送水记录不存在: " + id));
        d.setStatus(req.status());
        if (req.deliverer() != null) d.setDeliverer(req.deliverer());
        if (req.note() != null) d.setNote(req.note());
        return elderlyRepo.save(d);
    }

    // ---- 商户停业损失 ----

    public List<MerchantLoss> losses(Long eventId) {
        return eventId == null ? lossRepo.findAll() : lossRepo.findByEventId(eventId);
    }

    @Transactional
    public MerchantLoss addLoss(MerchantLossReq req) {
        MerchantLoss m = new MerchantLoss();
        m.setEvent(event(req.eventId()));
        m.setMerchantName(req.merchantName());
        m.setCategory(req.category());
        m.setLossAmount(req.lossAmount());
        m.setDescription(req.description());
        return lossRepo.save(m);
    }

    @Transactional
    public MerchantLoss updateLoss(long id, LossStatusReq req) {
        MerchantLoss m = lossRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "损失申报不存在: " + id));
        m.setStatus(req.status());
        if (req.handleNote() != null) m.setHandleNote(req.handleNote());
        return lossRepo.save(m);
    }

    // ---- 二次供水水箱 ----

    public List<SecondaryTank> tanks(Long eventId) {
        return eventId == null ? tankRepo.findAll() : tankRepo.findByEventId(eventId);
    }

    @Transactional
    public SecondaryTank addTank(TankReq req) {
        SecondaryTank t = new SecondaryTank();
        t.setEvent(event(req.eventId()));
        t.setCommunity(req.community());
        t.setBuilding(req.building());
        if (req.levelPercent() != null) t.setLevelPercent(req.levelPercent());
        if (req.status() != null) t.setStatus(req.status());
        t.setNote(req.note());
        t.setCheckedBy(currentUser.displayName());
        return tankRepo.save(t);
    }

    @Transactional
    public SecondaryTank updateTank(long id, TankUpdateReq req) {
        SecondaryTank t = tankRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "水箱记录不存在: " + id));
        if (req.levelPercent() != null) t.setLevelPercent(req.levelPercent());
        if (req.status() != null) t.setStatus(req.status());
        if (req.note() != null) t.setNote(req.note());
        t.setCheckedBy(currentUser.displayName());
        return tankRepo.save(t);
    }
}
