package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 现场进度记录：关阀、开挖、换管、冲洗、消毒、压力测试、道路恢复。
 */
@Getter
@Setter
@Entity
@Table(name = "progress_log")
public class ProgressLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private RepairOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepairStage stage;

    @Column(length = 512)
    private String note;

    @Column(nullable = false, length = 64)
    private String operatorName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
