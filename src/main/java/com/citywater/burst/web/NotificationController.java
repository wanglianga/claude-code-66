package com.citywater.burst.web;

import com.citywater.burst.config.CurrentUser;
import com.citywater.burst.dto.Requests.NotifyReq;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.ImpactAssessmentRepo;
import com.citywater.burst.repo.NotificationRepo;
import com.citywater.burst.repo.RepairOrderRepo;
import com.citywater.burst.service.BurstService;
import com.citywater.burst.service.NotifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepo notificationRepo;
    private final ImpactAssessmentRepo assessmentRepo;
    private final RepairOrderRepo orderRepo;
    private final BurstService burstService;
    private final NotifyService notifyService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<Notification> list(@RequestParam(required = false) Long eventId) {
        return eventId == null
                ? notificationRepo.findTop20ByOrderByCreatedAtDesc()
                : notificationRepo.findByEventIdOrderByCreatedAtDesc(eventId);
    }

    /** 客服手动发送通知（短信/电话/物业群/学校联系人/重点用户） */
    @PostMapping
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public Notification send(@Valid @RequestBody NotifyReq req) {
        BurstEvent e = burstService.get(req.eventId());
        RepairStage stage = orderRepo.findByEventIdOrderByCreatedAtDesc(e.getId()).stream()
                .findFirst().map(RepairOrder::getStage).orElse(RepairStage.DISPATCHED);
        Notification n = new Notification();
        n.setEvent(e);
        n.setChannel(req.channel());
        n.setAudience(req.audience());
        n.setContent(req.content());
        n.setStage(stage);
        n.setSendMode("MANUAL");
        n.setSentBy(currentUser.displayName());
        return notificationRepo.save(n);
    }

    /** 通知模板预览：内容随事件当前抢修阶段生成 */
    @GetMapping("/template")
    public Map<String, String> template(@RequestParam Long eventId, @RequestParam NotifyChannel channel) {
        BurstEvent e = burstService.get(eventId);
        RepairOrder o = orderRepo.findByEventIdOrderByCreatedAtDesc(e.getId()).stream()
                .findFirst().orElse(null);
        RepairStage stage = o != null ? o.getStage() : RepairStage.DISPATCHED;
        ImpactAssessment a = assessmentRepo.findByEventId(eventId).orElse(null);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("audience", notifyService.audienceFor(e, channel, a));
        m.put("content", notifyService.buildContent(e, o, stage));
        return m;
    }
}
