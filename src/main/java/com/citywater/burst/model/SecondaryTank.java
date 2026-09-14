package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 二次供水水箱状态：长时间停水后跟踪高层小区水箱水位，
 * 避免复供后仍有楼栋供水异常。
 */
@Getter
@Setter
@Entity
@Table(name = "secondary_tank")
public class SecondaryTank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private BurstEvent event;

    @Column(nullable = false, length = 128)
    private String community;

    @Column(nullable = false, length = 128)
    private String building;

    /** 水位百分比 0-100 */
    @Column(nullable = false)
    private Integer levelPercent = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TankStatus status = TankStatus.NORMAL;

    @Column(length = 64)
    private String checkedBy;

    @Column(length = 256)
    private String note;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
