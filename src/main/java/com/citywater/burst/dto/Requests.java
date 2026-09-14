package com.citywater.burst.dto;

import com.citywater.burst.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接口请求体集合。
 */
public final class Requests {

    private Requests() {}

    public record EventCreateReq(
            @NotNull EventSource source,
            @NotBlank String location,
            String description,
            Long pipeSegmentId,
            @NotNull Long zoneId,
            String reporterName,
            String reporterPhone) {}

    public record DispatchReq(
            @NotBlank String teamName,
            @NotBlank String crewLeader,
            String crewPhone,
            String valveOps,
            String excavationPermitNo,
            Boolean trafficControl,
            String trafficPlan,
            String spareParts,
            @NotNull Integer estimatedRestoreHours,
            String waterPoints,
            Boolean involveCs,
            Boolean involveStreet,
            Boolean involveProperty,
            Boolean involveWaterTruck,
            Boolean involveQuality) {}

    public record ProgressReq(@NotNull RepairStage stage, String note) {}

    public record RestoreCheckReq(
            Boolean pressureOk,
            Boolean qualityOk,
            Boolean flushingOk,
            Boolean notificationOk,
            Double turbidity,
            Double residualChlorine,
            String note) {}

    public record NotifyReq(
            @NotNull Long eventId,
            @NotNull NotifyChannel channel,
            @NotBlank String audience,
            @NotBlank String content) {}

    public record IssueCreateReq(
            @NotNull Long orderId,
            @NotNull IssueType type,
            @NotBlank String description,
            String contactName,
            String contactPhone) {}

    public record IssueStatusReq(@NotNull IssueStatus status, String handleNote) {}

    public record WaterPointReq(
            @NotNull Long eventId,
            @NotBlank String name,
            @NotBlank String location,
            String note) {}

    public record WaterPointUpdateReq(Integer queueLength, WaterPointStatus status, String note) {}

    public record ElderlyReq(
            @NotNull Long eventId,
            @NotBlank String elderName,
            @NotBlank String address,
            String phone,
            String deliverer,
            String note) {}

    public record AidStatusReq(@NotNull AidStatus status, String deliverer, String note) {}

    public record MerchantLossReq(
            @NotNull Long eventId,
            @NotBlank String merchantName,
            String category,
            BigDecimal lossAmount,
            String description) {}

    public record LossStatusReq(@NotNull LossStatus status, String handleNote) {}

    public record TankReq(
            @NotNull Long eventId,
            @NotBlank String community,
            @NotBlank String building,
            Integer levelPercent,
            TankStatus status,
            String note) {}

    public record TankUpdateReq(Integer levelPercent, TankStatus status, String note) {}

    /** 医院应急供水保障 */
    public record HospitalSupportCreateReq(
            @NotNull Long eventId,
            @NotBlank String hospitalName,
            Boolean needDialysis,
            Boolean needSurgery,
            Boolean needSterileSupply,
            Boolean needInpatient,
            String logisticsContact,
            String logisticsPhone) {}

    public record HospitalDispatchReq(@NotBlank String waterTrucks, String tempTanks) {}

    public record HospitalSupplyReq(@NotNull Double waterAmountM3, java.time.LocalDateTime restoreTime) {}

    public record AccessIssueReq(@NotBlank String altWaterPoint, String volunteers) {}

    public record HospitalConfirmReq(@NotBlank String hospitalConfirmer) {}

    public record ReviewReq(@NotBlank String reviewNote) {}

    /** 复供黄水投诉处理（水质检测点、楼栋高度、居民照片为必填关联资料） */
    public record YellowWaterCreateReq(
            @NotNull Long orderId,
            @NotNull ComplaintType complaintType,
            @NotBlank String community,
            String building,
            @NotNull Integer floors,
            String reporterName,
            String reporterPhone,
            String description,
            @NotBlank String samplePoint,
            @NotBlank String photoUrls,
            Long issueId) {}

    public record YellowWaterHandleReq(
            @NotNull YwMethod method,
            @NotBlank String note,
            Responsibility responsibility,
            String samplePoint) {}

    public record YellowWaterRecoveryReq(@NotNull Boolean recovered, String note) {}

    public record WatchClearReq(String note) {}

    /** 事件完整详情：评估 + 工单(进度/复供单/复供后问题) + 通知 + 停水保障 + 医疗保障 */
    public record EventDetail(
            BurstEvent event,
            ImpactAssessment assessment,
            List<OrderDetail> orders,
            List<Notification> notifications,
            List<WaterPoint> waterPoints,
            List<ElderlyDelivery> elderlyDeliveries,
            List<MerchantLoss> merchantLosses,
            List<SecondaryTank> tanks,
            List<HospitalSupport> hospitalSupports) {}

    public record OrderDetail(
            RepairOrder order,
            List<ProgressLog> logs,
            RestorationCheck check,
            List<PostRestoreIssue> issues,
            List<YellowWaterCase> yellowWaterCases) {}
}
