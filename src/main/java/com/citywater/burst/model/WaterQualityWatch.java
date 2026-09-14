package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 重点水质观察名单：反复投诉黄水/异味的小区自动进入观察。
 */
@Getter
@Setter
@Entity
@Table(name = "water_quality_watch")
public class WaterQualityWatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String community;

    /** 累计投诉次数 */
    @Column(nullable = false)
    private Integer complaintCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WatchStatus status = WatchStatus.WATCHING;

    @Column(length = 512)
    private String note;

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
