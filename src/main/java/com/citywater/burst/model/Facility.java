package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 分区内的重点对象：小区 / 医院 / 学校 / 餐饮街 / 道路 / 商业体。
 */
@Getter
@Setter
@Entity
@Table(name = "facility")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FacilityType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zone_id")
    private ValveZone zone;

    /** 影响人口（道路/餐饮街可为商户数） */
    @Column(nullable = false)
    private Integer population = 0;

    /** 是否高层（二次供水） */
    @Column(nullable = false)
    private Boolean highRise = false;

    @Column(length = 64)
    private String contactName;

    @Column(length = 32)
    private String contactPhone;

    @Column(length = 128)
    private String address;

    /** 备注，如 医院透析中心 / 学校午餐食堂 */
    @Column(length = 256)
    private String note;
}
