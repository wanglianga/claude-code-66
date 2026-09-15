package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 道路恢复验收单：工单进入道路恢复阶段自动创建。
 * 道路回填、围挡撤除、交通恢复、路面照片由现场负责人/城管/道路单位确认；
 * 验收不通过时围挡和交通提示继续保持；沉降责任可追溯施工班组、材料批次与恢复时间。
 */
@Getter
@Setter
@Entity
@Table(name = "road_restoration")
public class RoadRestoration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", unique = true)
    private RepairOrder order;

    /** 道路回填完成 */
    @Column(nullable = false)
    private Boolean backfillDone = false;

    /** 围挡撤除 */
    @Column(nullable = false)
    private Boolean barrierRemoved = false;

    /** 交通恢复 */
    @Column(nullable = false)
    private Boolean trafficRestored = false;

    /** 路面照片（URL，多个以逗号分隔） */
    @Column(length = 512)
    private String photoUrls;

    /** 施工班组（沉降责任追溯） */
    @Column(length = 64)
    private String constructionCrew;

    /** 材料批次，如 沥青批次AC-2026-0912（沉降责任追溯） */
    @Column(length = 64)
    private String materialBatch;

    /** 道路恢复时间（沉降责任追溯） */
    private LocalDateTime restoredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoadStatus status = RoadStatus.PENDING;

    /** 验收确认人 */
    @Column(length = 64)
    private String confirmer;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ConfirmerRole confirmerRole;

    private LocalDateTime confirmedAt;

    /** 验收不通过原因 */
    @Column(length = 256)
    private String rejectReason;

    /** 验收不通过时：围挡和交通提示继续保持 */
    @Column(nullable = false)
    private Boolean barrierMaintained = false;

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
