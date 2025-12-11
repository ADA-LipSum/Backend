package com.ada.proj.entity;

import java.time.Instant;

import com.ada.proj.enums.Role;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_uuid", columnNames = {"uuid"}),
                @UniqueConstraint(name = "uk_users_admin_id", columnNames = {"admin_id"}),
                @UniqueConstraint(name = "uk_users_custom_id", columnNames = {"custom_id"})
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(length = 36, nullable = false)
    private String uuid;

    @Column(name = "admin_id", length = 50, nullable = false)
    private String adminId;

    @Column(name = "custom_id", length = 50)
    private String customId;

    // 통합 비밀번호 컬럼
    @Column(name = "password", length = 255)
    private String password; // BCrypt 해시 저장

    // 마이그레이션 호환용(읽기 전용). 기존 custom_pw 값이 있는 경우 로그인 시 password로 이관합니다.
    @Column(name = "custom_pw", length = 255, insertable = false, updatable = false)
    private String legacyCustomPw;

    @Column(name = "user_realname", length = 10, nullable = false)
    private String userRealname;

    @Column(name = "user_nickname", length = 10, nullable = false)
    private String userNickname;

    @Column(name = "profile_image", length = 455)
    private String profileImage;

    @Column(name = "profile_banner", length = 455)
    private String profileBanner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // 닉네임을 이름으로 표시 여부 (요구 이미지의 토글 기능)
        @Column(name = "use_nickname", nullable = false)
        @Builder.Default
        private boolean useNickname = false;

    // 로그인 추적: 첫 로그인 여부 판단을 위해 사용
    @Column(name = "login_count", nullable = false)
    @Builder.Default
    private long loginCount = 0L;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
