package com.citywater.burst.web;

import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.*;
import com.citywater.burst.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 长时间停水保障：临时水点、老人送水、商户停业损失、二次供水水箱。
 */
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/water-points")
    public List<WaterPoint> waterPoints(@RequestParam(required = false) Long eventId) {
        return supportService.waterPoints(eventId);
    }

    @PostMapping("/water-points")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public WaterPoint addWaterPoint(@Valid @RequestBody WaterPointReq req) {
        return supportService.addWaterPoint(req);
    }

    @PostMapping("/water-points/{id}")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public WaterPoint updateWaterPoint(@PathVariable long id, @RequestBody WaterPointUpdateReq req) {
        return supportService.updateWaterPoint(id, req);
    }

    @GetMapping("/elderly")
    public List<ElderlyDelivery> elderly(@RequestParam(required = false) Long eventId) {
        return supportService.elderly(eventId);
    }

    @PostMapping("/elderly")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public ElderlyDelivery addElderly(@Valid @RequestBody ElderlyReq req) {
        return supportService.addElderly(req);
    }

    @PostMapping("/elderly/{id}/status")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public ElderlyDelivery updateElderly(@PathVariable long id, @Valid @RequestBody AidStatusReq req) {
        return supportService.updateElderly(id, req);
    }

    @GetMapping("/merchant-loss")
    public List<MerchantLoss> losses(@RequestParam(required = false) Long eventId) {
        return supportService.losses(eventId);
    }

    @PostMapping("/merchant-loss")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public MerchantLoss addLoss(@Valid @RequestBody MerchantLossReq req) {
        return supportService.addLoss(req);
    }

    @PostMapping("/merchant-loss/{id}/status")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public MerchantLoss updateLoss(@PathVariable long id, @Valid @RequestBody LossStatusReq req) {
        return supportService.updateLoss(id, req);
    }

    @GetMapping("/tanks")
    public List<SecondaryTank> tanks(@RequestParam(required = false) Long eventId) {
        return supportService.tanks(eventId);
    }

    @PostMapping("/tanks")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public SecondaryTank addTank(@Valid @RequestBody TankReq req) {
        return supportService.addTank(req);
    }

    @PostMapping("/tanks/{id}")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public SecondaryTank updateTank(@PathVariable long id, @RequestBody TankUpdateReq req) {
        return supportService.updateTank(id, req);
    }
}
