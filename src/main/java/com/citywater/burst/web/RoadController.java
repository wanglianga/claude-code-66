package com.citywater.burst.web;

import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.RoadRestoration;
import com.citywater.burst.model.SubsidenceReport;
import com.citywater.burst.service.RoadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 道路恢复验收与沉降复查。
 */
@RestController
@RequestMapping("/api/road")
@RequiredArgsConstructor
public class RoadController {

    private final RoadService roadService;

    @GetMapping
    public List<RoadRestoration> list() {
        return roadService.list();
    }

    /** 提交验收材料（现场/抢修队） */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('CREW','DISPATCHER','ADMIN')")
    public RoadRestoration submit(@PathVariable long id, @Valid @RequestBody RoadSubmitReq req) {
        return roadService.submit(id, req);
    }

    /** 验收通过（现场负责人/城管/道路单位确认） */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public RoadRestoration accept(@PathVariable long id, @Valid @RequestBody RoadAcceptReq req) {
        return roadService.accept(id, req);
    }

    /** 验收不通过（围挡和交通提示继续保持） */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public RoadRestoration reject(@PathVariable long id, @Valid @RequestBody RoadRejectReq req) {
        return roadService.reject(id, req);
    }

    @GetMapping("/subsidence")
    public List<SubsidenceReport> subsidenceList() {
        return roadService.subsidenceList();
    }

    /** 手动登记沉降（复查安排固定日期） */
    @PostMapping("/subsidence")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public SubsidenceReport addSubsidence(@Valid @RequestBody SubsidenceCreateReq req) {
        return roadService.addSubsidence(req.roadId(), req.description(), req.recheckDate(), null);
    }

    /** 沉降复查 */
    @PostMapping("/subsidence/{id}/recheck")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public SubsidenceReport recheck(@PathVariable long id, @Valid @RequestBody RecheckReq req) {
        return roadService.recheck(id, req);
    }
}
