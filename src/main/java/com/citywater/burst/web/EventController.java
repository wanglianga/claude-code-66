package com.citywater.burst.web;

import com.citywater.burst.dto.Requests.EventCreateReq;
import com.citywater.burst.dto.Requests.EventDetail;
import com.citywater.burst.model.BurstEvent;
import com.citywater.burst.model.EventStatus;
import com.citywater.burst.model.ImpactAssessment;
import com.citywater.burst.service.BurstService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final BurstService burstService;

    @GetMapping
    public List<BurstEvent> list(@RequestParam(required = false) EventStatus status) {
        return burstService.list(status);
    }

    @GetMapping("/{id}")
    public EventDetail detail(@PathVariable long id) {
        return burstService.detail(id);
    }

    /** 报警上报：管网传感器 / 居民报修 / 巡检员 */
    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public BurstEvent create(@Valid @RequestBody EventCreateReq req) {
        return burstService.create(req);
    }

    /** 影响评估：自动生成停水影响范围 */
    @PostMapping("/{id}/assess")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public ImpactAssessment assess(@PathVariable long id) {
        return burstService.assess(id);
    }

    /** 关闭事件（须已复供且复供后问题全部解决） */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public BurstEvent close(@PathVariable long id) {
        return burstService.close(id);
    }
}
