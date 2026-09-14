package com.citywater.burst.service;

import com.citywater.burst.config.CurrentUser;
import com.citywater.burst.dto.Requests.*;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 复供黄水/异味投诉处理：关联冲洗记录、水质检测点、楼栋高度与居民照片；
 * 客服安排二次冲洗/上门取样/解释短时排放，结论回写复供质量档案；
 * 二次冲洗后采集用户恢复情况；反复投诉小区进入重点水质观察；
 * 投诉集中在高层时提示物业检查二次供水设施，供水公司与物业责任分开记录。
 */
@Service
@RequiredArgsConstructor
public class YellowWaterService {

    /** 楼层达到该值视为高层（二次供水） */
    static final int HIGH_RISE_FLOORS = 7;
    /** 同小区投诉达到该次数进入重点水质观察 */
    static final int WATCH_THRESHOLD = 2;
    /** 同小区高层投诉达到该次数提示物业检查二次供水设施 */
    static final int HIGH_RISE_CONCENTRATION = 2;

    private final YellowWaterCaseRepo caseRepo;
    private final WaterQualityWatchRepo watchRepo;
    private final RepairOrderRepo orderRepo;
    private final ProgressLogRepo logRepo;
    private final RestorationCheckRepo checkRepo;
    private final PostRestoreIssueRepo issueRepo;
    private final NotificationRepo notificationRepo;
    private final CurrentUser currentUser;

    public List<YellowWaterCase> list(Long orderId, String community) {
        if (orderId != null) return caseRepo.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (community != null && !community.isBlank()) return caseRepo.findByCommunityOrderByCreatedAtDesc(community);
        return caseRepo.findAllByOrderByCreatedAtDesc();
    }

    public YellowWaterCase get(long id) {
        return caseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "黄水投诉单不存在: " + id));
    }

    @Transactional
    public YellowWaterCase create(YellowWaterCreateReq req) {
        // 关联资料门禁：水质检测点、楼栋高度、居民照片缺一不可（接口层 @Valid 之外的防御）
        if (req.samplePoint() == null || req.samplePoint().isBlank()
                || req.floors() == null
                || req.photoUrls() == null || req.photoUrls().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "缺少关联资料：水质检测点、楼栋高度、居民照片均为必填");
        }
        RepairOrder o = orderRepo.findById(req.orderId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "工单不存在: " + req.orderId()));
        EventStatus es = o.getEvent().getStatus();
        if (es != EventStatus.RESTORED && es != EventStatus.CLOSED) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "黄水投诉须在事件复供后登记，当前事件状态: " + es.getLabel());
        }
        YellowWaterCase c = new YellowWaterCase();
        c.setOrder(o);
        if (req.issueId() != null) {
            PostRestoreIssue issue = issueRepo.findById(req.issueId())
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "关联问题单不存在: " + req.issueId()));
            c.setIssue(issue);
        }
        c.setComplaintType(req.complaintType());
        c.setCommunity(req.community());
        c.setBuilding(req.building());
        c.setFloors(req.floors());
        c.setHighRise(req.floors() != null && req.floors() >= HIGH_RISE_FLOORS);
        c.setReporterName(req.reporterName());
        c.setReporterPhone(req.reporterPhone());
        c.setDescription(req.description());
        c.setSamplePoint(req.samplePoint());
        c.setPhotoUrls(req.photoUrls());
        // 关联冲洗记录：从抢修工单冲洗阶段自动快照
        c.setFlushRecord(snapshotFlushRecord(o));
        YellowWaterCase saved = caseRepo.save(c);

        // 反复投诉的小区进入重点水质观察
        long communityCount = caseRepo.countByCommunity(req.community());
        if (communityCount >= WATCH_THRESHOLD) {
            WaterQualityWatch w = watchRepo.findByCommunity(req.community()).orElseGet(() -> {
                WaterQualityWatch nw = new WaterQualityWatch();
                nw.setCommunity(req.community());
                nw.setComplaintCount(0);
                nw.setStatus(WatchStatus.WATCHING);
                nw.setNote("反复黄水/异味投诉，自动进入重点水质观察");
                return nw;
            });
            w.setComplaintCount((int) communityCount);
            if (w.getStatus() == WatchStatus.CLEARED) {
                w.setStatus(WatchStatus.WATCHING);
                w.setNote("观察解除后再次投诉，重新进入重点水质观察");
            }
            watchRepo.save(w);
        }

        // 投诉集中在高层：提示物业检查二次供水设施
        if (Boolean.TRUE.equals(saved.getHighRise())) {
            long highRiseCount = caseRepo.countByCommunityAndHighRiseTrue(req.community());
            if (highRiseCount >= HIGH_RISE_CONCENTRATION) {
                List<YellowWaterCase> communityCases = caseRepo.findByCommunityOrderByCreatedAtDesc(req.community());
                for (YellowWaterCase cc : communityCases) {
                    if (Boolean.TRUE.equals(cc.getHighRise()) && !Boolean.TRUE.equals(cc.getPropertyInspectAdvised())) {
                        cc.setPropertyInspectAdvised(true);
                        caseRepo.save(cc);
                    }
                }
                notifyProperty(o, req.community(), highRiseCount);
            }
        }
        return saved;
    }

    private String snapshotFlushRecord(RepairOrder o) {
        return logRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()).stream()
                .filter(l -> l.getStage() == RepairStage.FLUSHING)
                .findFirst()
                .map(l -> "冲洗记录：" + l.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                        + " " + (l.getNote() != null ? l.getNote() : "管道冲洗") + "（" + l.getOperatorName() + "）")
                .orElse("抢修工单无冲洗阶段记录");
    }

    private void notifyProperty(RepairOrder o, String community, long highRiseCount) {
        Notification n = new Notification();
        n.setEvent(o.getEvent());
        n.setChannel(NotifyChannel.PROPERTY_GROUP);
        n.setAudience(community + "物业");
        n.setContent("【二次供水提示】" + community + "黄水/异味投诉集中在高层（共" + highRiseCount
                + "起），请物业检查二次供水设施（水箱清洗、泵组运行）；供水公司与物业责任分开记录。");
        n.setStage(RepairStage.COMPLETED);
        n.setSendMode("AUTO");
        n.setSentBy("系统");
        notificationRepo.save(n);
    }

    /**
     * 客服安排处理：二次冲洗 / 上门取样 / 解释短时排放；结论回写复供质量档案。
     */
    @Transactional
    public YellowWaterCase handle(long id, YellowWaterHandleReq req) {
        YellowWaterCase c = get(id);
        if (c.getStatus() == YwStatus.DONE) {
            throw new ResponseStatusException(BAD_REQUEST, "该投诉已办结");
        }
        if (c.getStatus() == YwStatus.FOLLOW_UP) {
            throw new ResponseStatusException(BAD_REQUEST, "二次冲洗后待回访，请先采集用户恢复情况");
        }
        c.setMethod(req.method());
        c.setHandlingNote(req.note());
        if (req.responsibility() != null) {
            c.setResponsibility(req.responsibility());
        }
        if (req.samplePoint() != null && !req.samplePoint().isBlank()) {
            c.setSamplePoint(req.samplePoint());
        }
        c.setHandledBy(currentUser.displayName());
        c.setHandledAt(LocalDateTime.now());
        // 二次冲洗后需回访采集用户恢复情况，其余方式直接办结
        c.setStatus(req.method() == YwMethod.SECOND_FLUSH ? YwStatus.FOLLOW_UP : YwStatus.DONE);
        YellowWaterCase saved = caseRepo.save(c);

        appendToQualityArchive(c.getOrder(), "【黄水处理】" + c.getCommunity() + (c.getBuilding() != null ? c.getBuilding() : "")
                + "：" + req.method().getLabel() + "，" + req.note()
                + "（责任方：" + c.getResponsibility().getLabel() + "，" + currentUser.displayName() + "）");
        return saved;
    }

    /**
     * 二次冲洗后回访：采集用户是否恢复正常用水。
     */
    @Transactional
    public YellowWaterCase recordRecovery(long id, YellowWaterRecoveryReq req) {
        YellowWaterCase c = get(id);
        if (c.getStatus() != YwStatus.FOLLOW_UP) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "仅二次冲洗后的投诉需要回访采集，当前状态: " + c.getStatus().getLabel());
        }
        c.setRecovered(req.recovered());
        c.setRecoveredNote(req.note());
        c.setRecoveredAt(LocalDateTime.now());
        // 已恢复 -> 办结；未恢复 -> 重新待处理
        c.setStatus(Boolean.TRUE.equals(req.recovered()) ? YwStatus.DONE : YwStatus.OPEN);
        YellowWaterCase saved = caseRepo.save(c);

        appendToQualityArchive(c.getOrder(), "【黄水回访】" + c.getCommunity() + (c.getBuilding() != null ? c.getBuilding() : "")
                + "：二次冲洗后用户" + (Boolean.TRUE.equals(req.recovered()) ? "已恢复正常用水" : "仍未恢复正常，需继续处理")
                + (req.note() != null ? "，" + req.note() : ""));
        return saved;
    }

    /** 处理结论回写复供质量档案（复供确认单备注） */
    private void appendToQualityArchive(RepairOrder o, String line) {
        RestorationCheck check = checkRepo.findByOrderId(o.getId()).orElseGet(() -> {
            RestorationCheck nc = new RestorationCheck();
            nc.setOrder(o);
            return nc;
        });
        String existing = check.getNote() == null ? "" : check.getNote();
        String appended = existing.isEmpty() ? line : existing + "\n" + line;
        check.setNote(appended.length() > 1900 ? appended.substring(appended.length() - 1900) : appended);
        checkRepo.save(check);
    }

    // ---- 重点水质观察 ----

    public List<WaterQualityWatch> watchList() {
        return watchRepo.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional
    public WaterQualityWatch clearWatch(long id, WatchClearReq req) {
        WaterQualityWatch w = watchRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "观察记录不存在: " + id));
        w.setStatus(WatchStatus.CLEARED);
        w.setNote(req.note() != null ? req.note() : "水质持续合格，解除重点观察");
        return watchRepo.save(w);
    }
}
