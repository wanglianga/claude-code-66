package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 复供确认单：复供前必须确认水压、水质、管网冲洗、用户通知四项全部通过。
 */
@Getter
@Setter
@Entity
@Table(name = "restoration_check")
public class RestorationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", unique = true)
    private RepairOrder order;

    /** 水压测试合格 */
    @Column(nullable = false)
    private Boolean pressureOk = false;

    /** 水质检测合格 */
    @Column(nullable = false)
    private Boolean qualityOk = false;

    /** 管网冲洗完成 */
    @Column(nullable = false)
    private Boolean flushingOk = false;

    /** 用户通知到位 */
    @Column(nullable = false)
    private Boolean notificationOk = false;

    /** 浊度 NTU */
    private Double turbidity;

    /** 余氯 mg/L */
    private Double residualChlorine;

    @Column(length = 64)
    private String qualityInspector;

    @Column(length = 512)
    private String note;

    @Column(length = 64)
    private String confirmedBy;

    private LocalDateTime confirmedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
