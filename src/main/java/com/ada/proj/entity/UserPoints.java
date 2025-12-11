package com.ada.proj.entity;

import java.time.Instant;

import com.ada.proj.enums.PointChangeType;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_points",
        indexes = {
                @Index(name = "idx_user_points_user_uuid", columnList = "user_uuid"),
                @Index(name = "idx_user_points_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_points_points_uuid", columnNames = {"points_uuid"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "points_uuid", length = 36, nullable = false, unique = true)
    private String pointsUuid;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 10, nullable = false)
    private PointChangeType changeType;

    // 변화값 (+/-). GAIN은 양수, LOSS/USE는 음수
    @Column(name = "points", nullable = false)
    private Integer points;

    // 반영 후 잔액 스냅샷
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "ref_rule_id")
    private Long refRuleId;

    @Column(name = "ref_event_uuid", length = 36)
    private String refEventUuid;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
