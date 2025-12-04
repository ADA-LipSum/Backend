package com.ada.proj.entity;

import com.ada.proj.enums.JoinRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "study_group_join_request",
        indexes = {
            @Index(name = "idx_sgjr_group", columnList = "group_uuid"),
            @Index(name = "idx_sgjr_status", columnList = "status")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_sgjr_group_user", columnNames = {"group_uuid", "user_uuid"})
        })
public class StudyGroupJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_uuid", referencedColumnName = "group_uuid", nullable = false)
    private StudyGroup group;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private JoinRequestStatus status;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false, nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
