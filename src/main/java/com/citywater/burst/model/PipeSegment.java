package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 管段（历史管线资料）：口径、材质、敷设年份、所在道路。
 */
@Getter
@Setter
@Entity
@Table(name = "pipe_segment")
public class PipeSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @ManyToOne(optional = false)
    @JoinColumn(name = "zone_id")
    private ValveZone zone;

    /** 管径（毫米） */
    @Column(nullable = false)
    private Integer diameterMm;

    /** 管材，如 球墨铸铁 / PE / 铸铁 / 钢管 */
    @Column(nullable = false, length = 32)
    private String material;

    /** 敷设年份 */
    @Column(nullable = false)
    private Integer installYear;

    @Column(nullable = false, length = 128)
    private String roadName;
}
