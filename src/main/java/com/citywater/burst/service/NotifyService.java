package com.citywater.burst.service;

import com.citywater.burst.model.*;
import com.citywater.burst.repo.ImpactAssessmentRepo;
import com.citywater.burst.repo.NotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客服通知：内容随抢修进展（阶段）变化，渠道覆盖短信、电话、物业群、
 * 学校联系人、重点用户。阶段推进时系统自动生成对应通知。
 */
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotificationRepo notificationRepo;
    private final ImpactAssessmentRepo assessmentRepo;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM月dd日HH时");

    /**
     * 按事件当前状态与抢修阶段生成通知内容模板。
     */
    public String buildContent(BurstEvent e, RepairOrder o, RepairStage stage) {
        String loc = e.getLocation();
        String eta = o != null && o.getEstimatedRestoreTime() != null
                ? o.getEstimatedRestoreTime().format(TIME_FMT) : "待定";
        String waterPoint = o != null && o.getWaterPoints() != null && !o.getWaterPoints().isBlank()
                ? o.getWaterPoints() : "另行通知";
        return switch (stage) {
            case DISPATCHED -> "【停水通知】因" + loc + "供水管道爆裂，" + e.getZone().getName()
                    + "区域将暂停供水进行抢修，预计" + eta + "恢复。应急送水点：" + waterPoint
                    + "。请提前储水，给您带来不便敬请谅解。";
            case ARRIVED -> "【抢修进展】" + loc + "爆管抢修队已到达现场，正在勘察并准备关阀开挖，预计" + eta + "恢复供水。";
            case VALVE_CLOSED -> "【抢修进展】" + loc + "相关阀门已关闭止水，停水范围已确定，正在组织开挖，预计" + eta + "恢复供水。应急送水点：" + waterPoint + "。";
            case EXCAVATION -> "【抢修进展】" + loc + "正在开挖作业，请周边车辆行人注意避让，预计" + eta + "恢复供水。";
            case PIPE_REPLACED -> "【抢修进展】" + loc + "破损管段已更换完成，即将进行管道冲洗消毒，预计" + eta + "恢复供水。";
            case FLUSHING -> "【抢修进展】" + loc + "管道冲洗中，请勿提前开启家中阀门，预计" + eta + "恢复供水。";
            case DISINFECTION -> "【抢修进展】" + loc + "管道消毒中，水质检测人员已同步取样，预计" + eta + "恢复供水。";
            case PRESSURE_TEST -> "【抢修进展】" + loc + "管道压力测试中，即将恢复供水，请留意后续复供通知。";
            case ROAD_RESTORED -> "【抢修进展】" + loc + "路面恢复中，供水复供前确认同步进行，请耐心等待。";
            case COMPLETED -> "【复供通知】" + loc + "爆管抢修完成，供水已恢复。初期可能出现短暂黄水，请放水至清澈后使用；高层二次供水楼栋恢复可能稍有延迟。如有用水问题请拨打供水服务热线。";
        };
    }

    /**
     * 渠道对应的目标人群（根据影响评估自动生成）。
     */
    public String audienceFor(BurstEvent e, NotifyChannel channel, ImpactAssessment a) {
        List<Facility> facilities = a != null ? a.getAffectedFacilities() : List.of();
        return switch (channel) {
            case SMS -> joinNames(facilities, FacilityType.COMMUNITY, "受影响小区住户");
            case PROPERTY_GROUP -> joinNames(facilities, FacilityType.COMMUNITY, "各小区物业群");
            case SCHOOL_CONTACT -> joinNames(facilities, FacilityType.SCHOOL, "学校联系人");
            case PHONE -> joinNames(facilities, FacilityType.HOSPITAL, "重点单位电话通知");
            case KEY_USER -> {
                List<String> keys = new ArrayList<>();
                facilities.stream()
                        .filter(f -> f.getType() == FacilityType.HOSPITAL || Boolean.TRUE.equals(f.getHighRise()))
                        .forEach(f -> keys.add(f.getName() + (f.getContactName() != null ? "(" + f.getContactName() + ")" : "")));
                yield keys.isEmpty() ? "重点用户名单" : String.join("、", keys);
            }
        };
    }

    private String joinNames(List<Facility> facilities, FacilityType type, String fallback) {
        String names = facilities.stream()
                .filter(f -> f.getType() == type)
                .map(Facility::getName)
                .collect(Collectors.joining("、"));
        return names.isEmpty() ? fallback : names;
    }

    /**
     * 阶段推进时自动生成通知：按影响评估决定覆盖哪些渠道。
     */
    public List<Notification> autoNotify(BurstEvent e, RepairOrder o, RepairStage stage, String actor) {
        ImpactAssessment a = assessmentRepo.findByEventId(e.getId()).orElse(null);
        List<NotifyChannel> channels = new ArrayList<>();
        channels.add(NotifyChannel.SMS);
        channels.add(NotifyChannel.PROPERTY_GROUP);
        if (a != null) {
            if (Boolean.TRUE.equals(a.getHospitalAffected())) {
                channels.add(NotifyChannel.PHONE);
                channels.add(NotifyChannel.KEY_USER);
            }
            if (Boolean.TRUE.equals(a.getSchoolAffected())) {
                channels.add(NotifyChannel.SCHOOL_CONTACT);
            }
            if (Boolean.TRUE.equals(a.getHighRiseAffected()) && !channels.contains(NotifyChannel.KEY_USER)) {
                channels.add(NotifyChannel.KEY_USER);
            }
        }
        String content = buildContent(e, o, stage);
        List<Notification> saved = new ArrayList<>();
        for (NotifyChannel ch : channels) {
            Notification n = new Notification();
            n.setEvent(e);
            n.setChannel(ch);
            n.setAudience(audienceFor(e, ch, a));
            n.setContent(content);
            n.setStage(stage);
            n.setSendMode("AUTO");
            n.setSentBy(actor);
            saved.add(notificationRepo.save(n));
        }
        return saved;
    }
}
