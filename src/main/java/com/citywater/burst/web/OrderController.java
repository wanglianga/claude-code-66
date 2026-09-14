package com.citywater.burst.web;

import com.citywater.burst.dto.Requests.DispatchReq;
import com.citywater.burst.dto.Requests.ProgressReq;
import com.citywater.burst.dto.Requests.RestoreCheckReq;
import com.citywater.burst.model.ProgressLog;
import com.citywater.burst.model.RepairOrder;
import com.citywater.burst.model.RestorationCheck;
import com.citywater.burst.service.RepairService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final RepairService repairService;

    @GetMapping("/orders")
    public List<RepairOrder> list() {
        return repairService.listOrders();
    }

    @GetMapping("/orders/{id}")
    public RepairOrder get(@PathVariable long id) {
        return repairService.getOrder(id);
    }

    /** 调度派单 */
    @PostMapping("/events/{eventId}/dispatch")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public RepairOrder dispatch(@PathVariable long eventId, @Valid @RequestBody DispatchReq req) {
        return repairService.dispatch(eventId, req);
    }

    /** 现场进度上报（抢修队/调度） */
    @PostMapping("/orders/{id}/progress")
    @PreAuthorize("hasAnyRole('CREW','DISPATCHER','ADMIN')")
    public ProgressLog progress(@PathVariable long id, @Valid @RequestBody ProgressReq req) {
        return repairService.addProgress(id, req);
    }

    /** 复供前确认单登记（水质检测员/调度） */
    @PostMapping("/orders/{id}/restore-check")
    @PreAuthorize("hasAnyRole('QUALITY','DISPATCHER','ADMIN')")
    public RestorationCheck upsertCheck(@PathVariable long id, @RequestBody RestoreCheckReq req) {
        return repairService.upsertCheck(id, req);
    }

    /** 复供放行（四项确认全部通过才允许） */
    @PostMapping("/orders/{id}/confirm-restore")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public RepairOrder confirmRestore(@PathVariable long id) {
        return repairService.confirmRestore(id);
    }
}
