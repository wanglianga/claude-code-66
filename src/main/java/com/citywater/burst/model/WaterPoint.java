package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 应急送水点：长时间停水时设立，记录排队情况。
 */
@Getter
@Setter
@Entity
@Table(name = "water_point")
public class WaterPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private BurstEvent event;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 128)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WaterPointStatus status = WaterPointStatus.OPEN;

    /** 当前排队人数 */
    @Column(nullable = false)
    private Integer queueLength = 0;

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
