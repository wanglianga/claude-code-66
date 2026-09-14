package com.citywater.burst.web;

import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.HospitalSupport;
import com.citywater.burst.service.HospitalSupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医院应急供水保障。
 */
@RestController
@RequestMapping("/api/hospital-support")
@RequiredArgsConstructor
public class HospitalSupportController {

    private final HospitalSupportService service;

    @GetMapping
    public List<HospitalSupport> list(@RequestParam(required = false) Long eventId) {
        return service.list(eventId);
    }

    /** 手动登记保障单（评估未覆盖的医院） */
    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public HospitalSupport create(@Valid @RequestBody HospitalSupportCreateReq req) {
        return service.create(req);
    }

    /** 调度供水车与临时水箱 */
    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public HospitalSupport dispatch(@PathVariable long id, @Valid @RequestBody HospitalDispatchReq req) {
        return service.dispatchSupply(id, req);
    }

    /** 供水到位（记录到位时间与用水量） */
    @PostMapping("/{id}/supply")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public HospitalSupport supply(@PathVariable long id, @Valid @RequestBody HospitalSupplyReq req) {
        return service.markSupplied(id, req);
    }

    /** 供水车无法进入院区：改设水点 + 志愿者送水 */
    @PostMapping("/{id}/access-issue")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public HospitalSupport accessIssue(@PathVariable long id, @Valid @RequestBody AccessIssueReq req) {
        return service.recordAccessIssue(id, req);
    }

    /** 医院确认（客服端随后显示医疗用水保障完成） */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public HospitalSupport confirm(@PathVariable long id, @Valid @RequestBody HospitalConfirmReq req) {
        return service.confirm(id, req);
    }

    /** 复盘记录 */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public HospitalSupport review(@PathVariable long id, @Valid @RequestBody ReviewReq req) {
        return service.addReview(id, req);
    }
}
