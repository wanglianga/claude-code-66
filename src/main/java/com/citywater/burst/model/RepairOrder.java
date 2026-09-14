package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 抢修工单（调度派单）：同步阀门位置、开挖许可、交通协管、备件库存、
 * 预计停水时间、应急送水点，并把抢修队/客服/街道/物业/供水车/水质检测
 * 放在同一事件里推进。
 */
@Getter
@Setter
@Entity
@Table(name = "repair_order")
public class RepairOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private BurstEvent event;

    @Column(nullable = false, length = 64)
    private String teamName;

    @Column(nullable = false, length = 64)
    private String crewLeader;

    @Column(length = 32)
    private String crewPhone;

    /** 阀门位置与关阀操作安排 */
    @Column(length = 512)
    private String valveOps;

    /** 开挖许可编号 */
    @Column(length = 64)
    private String excavationPermitNo;

    /** 是否需要交通协管 */
    @Column(nullable = false)
    private Boolean trafficControl = false;

    @Column(length = 256)
    private String trafficPlan;

    /** 备件库存确认（管段/管件/消毒剂） */
    @Column(length = 256)
    private String spareParts;

    /** 预计恢复供水时间 */
    private LocalDateTime estimatedRestoreTime;

    /** 应急送水点安排 */
    @Column(length = 256)
    private String waterPoints;

    /** 协同方：客服 / 街道 / 物业 / 供水车 / 水质检测 */
    @Column(nullable = false)
    private Boolean involveCs = false;

    @Column(nullable = false)
    private Boolean involveStreet = false;

    @Column(nullable = false)
    private Boolean involveProperty = false;

    @Column(nullable = false)
    private Boolean involveWaterTruck = false;

    @Column(nullable = false)
    private Boolean involveQuality = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepairStage stage = RepairStage.DISPATCHED;

    @Column(length = 64)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
