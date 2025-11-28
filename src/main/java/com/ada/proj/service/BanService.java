// src/main/java/com/ada/proj/service/BanService.java
package com.ada.proj.service;

import com.ada.proj.dto.BanCreateRequest;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserBan;
import com.ada.proj.repository.UserBanRepository;
import com.ada.proj.repository.UserRepository;

import com.ada.proj.exception.BanExpiresInvalidException;
import com.ada.proj.exception.AlreadyBannedException;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BanService {

    private final UserRepository userRepo;
    private final UserBanRepository banRepo;

    @Transactional
    public void giveBan(BanCreateRequest req) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || (auth.getPrincipal() instanceof String
                && "anonymousUser".equals(auth.getPrincipal()))) {
            throw new AccessDeniedException("인증되지 않은 요청입니다.");
        }

        String adminUuid = auth.getName();

        User admin = userRepo.findByUuid(adminUuid)
                .orElseThrow(() -> new AccessDeniedException("관리자 계정을 찾을 수 없습니다."));

        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("제재 권한이 없습니다.");
        }

        User target = userRepo.findByUuid(req.getTargetUuid())
                .orElseThrow(() -> new RuntimeException("제재 대상 유저를 찾을 수 없습니다."));

        // ===========================
        // (1) 이미 active 제재가 있으면 금지
        // ===========================
        UserBan existing = banRepo.findByTargetUserAndActiveIsTrue(target).orElse(null);
        if (existing != null) {
            throw new AlreadyBannedException("이미 제재 중인 유저입니다.");
        }

        // ===========================
        // (2) expiresAt 유효성 검사
        // ===========================
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = req.getExpiresAt();

        // expiresAt != null && (expiresAt <= now)
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new BanExpiresInvalidException("만료 시간은 현재 시간보다 이후여야 합니다.");
        }

        // ===========================
        // (3) 신규 제재 생성
        // ===========================
        UserBan ban = UserBan.builder()
                .adminUser(admin)
                .targetUser(target)
                .reason(req.getReason())
                .bannedAt(now)
                .expiresAt(expiresAt)
                .active(true)
                .build();

        banRepo.save(ban);
    }

    @Transactional
    public void releaseBanManual(Long banId) {

        UserBan ban = banRepo.findById(banId)
                .orElseThrow(() -> new RuntimeException("제재 내역 없음"));

        ban.setActive(false);
        banRepo.save(ban);
    }

    @Transactional
    public void releaseBan(String userUuid) {

        User target = userRepo.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        UserBan ban = banRepo.findByTargetUserAndActiveIsTrue(target)
                .orElseThrow(() -> new RuntimeException("활성 제재 없음"));

        ban.setActive(false);
        banRepo.save(ban);
    }
}