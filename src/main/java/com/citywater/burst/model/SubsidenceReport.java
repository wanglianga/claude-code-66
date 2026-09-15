package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 沉降记录：关联道路恢复验收单（责任追溯施工班组/材料批次/恢复时间），
 * 复查安排固定日期；周边商户和居民投诉自动关联到道路恢复记录。
 */
@Getter
@Setter
@Entity
@Table(name = "subsidence_report")
public class SubsidenceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "road_restoration_id")
    private RoadRestoration roadRestoration;

    /** 关联的投诉单（自动关联周边商户/居民投诉） */
    @ManyToOne
    @JoinColumn(name = "issue_id")
    private PostRestoreIssue issue;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(nullable = false)
    private LocalDateTime reportDate;

    /** 沉降复查固定日期 */
    @Column(nullable = false)
    private LocalDate recheckDate;

    @Column(length = 512)
    private String recheckResult;

    private LocalDateTime recheckedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubsidenceStatus status = SubsidenceStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (reportDate == null) {
            reportDate = LocalDateTime.now();
        }
    }
}
