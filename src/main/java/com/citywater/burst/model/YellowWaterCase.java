package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 复供黄水/异味投诉处理单：挂在原抢修单下，关联冲洗记录、水质检测点、
 * 楼栋高度与居民照片；客服安排二次冲洗/上门取样/解释短时排放，处理结论
 * 回写复供质量档案；二次冲洗后采集用户是否恢复正常用水。
 */
@Getter
@Setter
@Entity
@Table(name = "yellow_water_case")
public class YellowWaterCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 原抢修单 */
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private RepairOrder order;

    /** 关联的复供后问题单（可选） */
    @ManyToOne
    @JoinColumn(name = "issue_id")
    private PostRestoreIssue issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ComplaintType complaintType;

    @Column(nullable = false, length = 128)
    private String community;

    @Column(length = 128)
    private String building;

    /** 楼栋高度（层数），>=7 层视为高层（二次供水） */
    private Integer floors;

    @Column(nullable = false)
    private Boolean highRise = false;

    @Column(length = 64)
    private String reporterName;

    @Column(length = 32)
    private String reporterPhone;

    @Column(length = 512)
    private String description;

    /** 居民照片（URL，多个以逗号分隔） */
    @Column(length = 512)
    private String photoUrls;

    /** 关联冲洗记录（创建时从抢修工单冲洗阶段自动快照） */
    @Column(length = 512)
    private String flushRecord;

    /** 水质检测点，如 3栋2单元801厨房水龙头 / 小区总表 */
    @Column(length = 128)
    private String samplePoint;

    /** 处理方式 */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private YwMethod method;

    /** 处理说明与结论（回写复供质量档案） */
    @Column(length = 512)
    private String handlingNote;

    @Column(length = 64)
    private String handledBy;

    private LocalDateTime handledAt;

    /** 责任方：供水公司与物业分开记录 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Responsibility responsibility = Responsibility.UNDETERMINED;

    /** 投诉集中在高层时已提示物业检查二次供水设施 */
    @Column(nullable = false)
    private Boolean propertyInspectAdvised = false;

    /** 二次冲洗后回访：用户是否恢复正常用水 */
    private Boolean recovered;

    @Column(length = 256)
    private String recoveredNote;

    private LocalDateTime recoveredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private YwStatus status = YwStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
