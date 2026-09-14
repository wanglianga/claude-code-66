package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 影响评估：根据管径、阀门分区、周边小区/医院/学校、道路交通和历史管线资料
 * 自动生成的停水影响范围分析。
 */
@Getter
@Setter
@Entity
@Table(name = "impact_assessment")
public class ImpactAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "event_id", unique = true)
    private BurstEvent event;

    /** 受影响人口 */
    @Column(nullable = false)
    private Integer affectedPopulation = 0;

    /** 受影响户数（按 3 人/户估算） */
    @Column(nullable = false)
    private Integer affectedHouseholds = 0;

    @Column(nullable = false)
    private Boolean hospitalAffected = false;

    @Column(nullable = false)
    private Boolean schoolAffected = false;

    @Column(nullable = false)
    private Boolean highRiseAffected = false;

    @Column(nullable = false)
    private Boolean restaurantAffected = false;

    @Column(nullable = false)
    private Boolean roadAffected = false;

    /** 受影响设施清单 */
    @ManyToMany
    @JoinTable(name = "assessment_facility",
            joinColumns = @JoinColumn(name = "assessment_id"),
            inverseJoinColumns = @JoinColumn(name = "facility_id"))
    private List<Facility> affectedFacilities = new ArrayList<>();

    /** 建议关阀方案（根据分区阀门台账生成） */
    @Column(length = 512)
    private String valvePlan;

    /** 历史管线资料分析（管龄、材质风险） */
    @Column(length = 512)
    private String pipeAnalysis;

    /** 交通影响分析 */
    @Column(length = 512)
    private String trafficImpact;

    /** 建议级别 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Severity suggestedSeverity = Severity.MODERATE;

    /** 预计停水时长（小时） */
    @Column(nullable = false)
    private Integer estimatedOutageHours = 4;

    @Column(length = 64)
    private String assessor;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
