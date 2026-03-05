package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_data",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_user_data_uuid", columnNames = {"uuid"})
        })
public class UserData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(length = 36, nullable = false)
    private String uuid; // FK -> users.uuid

    @Column(length = 255)
    private String intro;

    @Column(name = "tech_stack", length = 255)
    private String techStack;

    @Column(length = 50)
    private String badge;

    @Column(name = "activity_score", nullable = false)
    @Builder.Default
    private Integer activityScore = 0;

    @Column(name = "contribution_data", columnDefinition = "JSON")
    private String contributionData;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
