package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 客服通知：覆盖短信、电话、物业群、学校联系人、重点用户，
 * 内容随抢修进展（阶段）变化。
 */
@Getter
@Setter
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private BurstEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotifyChannel channel;

    /** 目标人群，如 阳光高层小区住户 / 人民医院透析中心 */
    @Column(nullable = false, length = 128)
    private String audience;

    @Column(nullable = false, length = 1024)
    private String content;

    /** 发送时所处的抢修阶段 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepairStage stage;

    /** AUTO=系统随阶段自动生成，MANUAL=客服手动发送 */
    @Column(nullable = false, length = 16)
    private String sendMode = "MANUAL";

    @Column(nullable = false, length = 64)
    private String sentBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
