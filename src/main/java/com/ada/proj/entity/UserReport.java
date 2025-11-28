// src/main/java/com/ada/proj/entity/UserReport.java
package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_uuid")
    private User reporter;

    // 신고 대상
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_uuid")
    private User targetUser;

    private String reason;       // 신고 사유
    private String reportType;   // POST / COMMENT / USER
    private LocalDateTime reportedAt;
}