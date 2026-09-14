package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户停业损失申报（餐饮街等），作为赔付依据。
 */
@Getter
@Setter
@Entity
@Table(name = "merchant_loss")
public class MerchantLoss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private BurstEvent event;

    @Column(nullable = false, length = 128)
    private String merchantName;

    /** 经营类别，如 餐饮 / 洗浴 / 洗车 */
    @Column(length = 64)
    private String category;

    /** 申报损失金额（元） */
    @Column(precision = 12, scale = 2)
    private BigDecimal lossAmount;

    @Column(length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LossStatus status = LossStatus.REPORTED;

    @Column(length = 512)
    private String handleNote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
