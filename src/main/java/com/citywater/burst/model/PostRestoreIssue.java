package com.citywater.burst.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 复供后问题：黄水投诉、二次漏水、道路沉降、赔付申请，
 * 全部回到原抢修单形成闭环。
 */
@Getter
@Setter
@Entity
@Table(name = "post_restore_issue")
public class PostRestoreIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private RepairOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IssueType type;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(length = 64)
    private String contactName;

    @Column(length = 32)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IssueStatus status = IssueStatus.OPEN;

    @Column(length = 512)
    private String handleNote;

    @Column(length = 64)
    private String handler;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
