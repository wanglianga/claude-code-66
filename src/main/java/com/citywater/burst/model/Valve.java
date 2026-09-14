package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 阀门台账：位置与所属分区，用于生成关阀方案。
 */
@Getter
@Setter
@Entity
@Table(name = "valve")
public class Valve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String location;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zone_id")
    private ValveZone zone;

    @Column(nullable = false)
    private Boolean normallyOpen = true;
}
