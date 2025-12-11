package com.ada.proj.entity;

import java.time.Instant;

import com.ada.proj.enums.PointChangeType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "points_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_points_rule_code", columnNames = {"rule_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "rule_code", length = 50, nullable = false)
    private String ruleCode; // e.g., SIGNUP, DAILY_LOGIN

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 10, nullable = false)
    private PointChangeType changeType; // GAIN or LOSS

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
