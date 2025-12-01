// src/main/java/com/ada/proj/entity/UserBan.java
package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_ban")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제재 대상
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User targetUser;

    // 제재 부여자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_seq", nullable = false)
    private User adminUser;

    @Column(nullable = false)
    private String reason;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;   // UTC 기준 저장

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;  // UTC 기준 저장

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @PrePersist
    public void onCreate() {
        // 제재가 처음 저장되는 순간, 자동으로 현재 시간 저장
        if (bannedAt == null) {
            bannedAt = LocalDateTime.now();
        }
        // active 기본값 자동 처리
        if (active == null) {
            active = true;
        }
    }
}
