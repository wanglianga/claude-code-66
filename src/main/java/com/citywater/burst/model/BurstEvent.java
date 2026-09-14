package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 爆管事件：从报警（传感器/居民/巡检）到复供关闭的全流程主单。
 */
@Getter
@Setter
@Entity
@Table(name = "burst_event")
public class BurstEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String eventNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventSource source;

    @Column(nullable = false, length = 128)
    private String location;

    @Column(length = 512)
    private String description;

    @ManyToOne
    @JoinColumn(name = "pipe_segment_id")
    private PipeSegment pipeSegment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zone_id")
    private ValveZone zone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Severity severity = Severity.MODERATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventStatus status = EventStatus.REPORTED;

    @Column(length = 64)
    private String reporterName;

    @Column(length = 32)
    private String reporterPhone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime restoredAt;

    private LocalDateTime closedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
