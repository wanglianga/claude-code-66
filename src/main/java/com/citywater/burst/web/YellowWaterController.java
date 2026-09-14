package com.citywater.burst.web;

import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.WaterQualityWatch;
import com.citywater.burst.model.YellowWaterCase;
import com.citywater.burst.service.YellowWaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 复供黄水/异味投诉处理。
 */
@RestController
@RequestMapping("/api/yellow-water")
@RequiredArgsConstructor
public class YellowWaterController {

    private final YellowWaterService service;

    @GetMapping
    public List<YellowWaterCase> list(@RequestParam(required = false) Long orderId,
                                      @RequestParam(required = false) String community) {
        return service.list(orderId, community);
    }

    /** 登记黄水/异味投诉（关联冲洗记录、检测点、楼栋高度、居民照片） */
    @PostMapping
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public YellowWaterCase create(@Valid @RequestBody YellowWaterCreateReq req) {
        return service.create(req);
    }

    /** 客服安排处理：二次冲洗 / 上门取样 / 解释短时排放 */
    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public YellowWaterCase handle(@PathVariable long id, @Valid @RequestBody YellowWaterHandleReq req) {
        return service.handle(id, req);
    }

    /** 二次冲洗后回访：采集用户是否恢复正常用水 */
    @PostMapping("/{id}/recovery")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public YellowWaterCase recovery(@PathVariable long id, @Valid @RequestBody YellowWaterRecoveryReq req) {
        return service.recordRecovery(id, req);
    }

    /** 重点水质观察名单 */
    @GetMapping("/watch")
    public List<WaterQualityWatch> watchList() {
        return service.watchList();
    }

    /** 解除重点观察 */
    @PostMapping("/watch/{id}/clear")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public WaterQualityWatch clearWatch(@PathVariable long id, @RequestBody(required = false) WatchClearReq req) {
        return service.clearWatch(id, req == null ? new WatchClearReq(null) : req);
    }
}
