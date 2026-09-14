package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 医院应急供水保障单：爆管影响医院供水时，优先识别透析、手术、消毒供应、
 * 住院楼需求，调度供水车与临时水箱，对接医院后勤联系人。
 * 供水到位、用水量、复供时间随保障单写回抢修事件；供水车无法进入院区时
 * 记录改设水点、志愿者送水与医院确认人，保障过程纳入复盘。
 */
@Getter
@Setter
@Entity
@Table(name = "hospital_support")
public class HospitalSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private BurstEvent event;

    @Column(nullable = false, length = 128)
    private String hospitalName;

    /** 优先识别需求：透析 / 手术 / 消毒供应 / 住院楼 */
    @Column(nullable = false)
    private Boolean needDialysis = true;

    @Column(nullable = false)
    private Boolean needSurgery = true;

    @Column(nullable = false)
    private Boolean needSterileSupply = true;

    @Column(nullable = false)
    private Boolean needInpatient = true;

    /** 医院后勤联系人 */
    @Column(length = 64)
    private String logisticsContact;

    @Column(length = 32)
    private String logisticsPhone;

    /** 调度供水车，如 供水车2辆（浙A·D1234、浙A·D5678） */
    @Column(length = 256)
    private String waterTrucks;

    /** 临时水箱，如 5m³×2（住院楼前） */
    @Column(length = 256)
    private String tempTanks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HospitalSupportStatus status = HospitalSupportStatus.PENDING;

    /** 供水到位时间（写入抢修事件） */
    private LocalDateTime arrivedAt;

    /** 累计供水量 m³（写入抢修事件） */
    private Double waterAmountM3;

    /** 复供时间（写入抢修事件，事件复供时自动回填） */
    private LocalDateTime restoreTime;

    /** 供水车无法进入院区 */
    @Column(nullable = false)
    private Boolean truckAccessIssue = false;

    /** 改设水点 */
    @Column(length = 256)
    private String altWaterPoint;

    /** 志愿者送水安排 */
    @Column(length = 256)
    private String volunteers;

    /** 医院确认人 */
    @Column(length = 64)
    private String hospitalConfirmer;

    /** 医院确认时间 */
    private LocalDateTime confirmedAt;

    /** 复盘记录（保障过程纳入复盘） */
    @Column(length = 512)
    private String reviewNote;

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
