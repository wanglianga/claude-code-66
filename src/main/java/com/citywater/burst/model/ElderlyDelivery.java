package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 老人/行动不便用户上门送水记录。
 */
@Getter
@Setter
@Entity
@Table(name = "elderly_delivery")
public class ElderlyDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private BurstEvent event;

    @Column(nullable = false, length = 64)
    private String elderName;

    @Column(nullable = false, length = 128)
    private String address;

    @Column(length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AidStatus status = AidStatus.PENDING;

    /** 送水人（志愿者/物业/供水车人员） */
    @Column(length = 64)
    private String deliverer;

    @Column(length = 256)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
