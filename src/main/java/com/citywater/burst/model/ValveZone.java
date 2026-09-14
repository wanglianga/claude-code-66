package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 阀门分区：管网按阀门划分的供水分区，是判断停水影响范围的基本单元。
 */
@Getter
@Setter
@Entity
@Table(name = "valve_zone")
public class ValveZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;
}
