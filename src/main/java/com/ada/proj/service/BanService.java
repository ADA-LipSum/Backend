// src/main/java/com/ada/proj/service/BanService.java
package com.ada.proj.service;

import com.ada.proj.dto.BanInfoResponse;
import com.ada.proj.dto.BanRequest;
import com.ada.proj.dto.BanResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserBan;
import com.ada.proj.enums.DurationUnit;
import com.ada.proj.exception.AlreadyBannedException;
import com.ada.proj.repository.UserBanRepository;
import com.ada.proj.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BanService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private final UserRepository userRepo;
    private final UserBanRepository banRepo;

    @Transactional
    public BanResponse createBan(BanRequest req) {

        User admin = requireBanPrivilege();

        User target = userRepo.findByUuid(req.getTargetUuid())
                .orElseThrow(() -> new RuntimeException("제재 대상 유저를 찾을 수 없습니다."));

        if (req.getDurationValue() <= 0) {
            throw new IllegalArgumentException("durationValue는 0보다 커야 합니다.");
        }

        DurationUnit unit = Objects.requireNonNull(req.getDurationUnit(), "durationUnit is required");

        UserBan existing = banRepo.findByTargetUserAndActiveIsTrue(target).orElse(null);
        if (existing != null) {
            throw new AlreadyBannedException("이미 제재 중인 유저입니다.");
        }

        LocalDateTime nowKst = LocalDateTime.now(KST);
        LocalDateTime expiresAtKst = calculateExpiresAt(nowKst, req.getDurationValue(), unit);
        LocalDateTime expiresAtUtc = ZonedDateTime.of(expiresAtKst, KST).withZoneSameInstant(UTC).toLocalDateTime();

        UserBan ban = UserBan.builder()
                .adminUser(admin)
                .targetUser(target)
                .reason(req.getReason())
                .bannedAt(ZonedDateTime.of(nowKst, KST).withZoneSameInstant(UTC).toLocalDateTime())
                .expiresAt(expiresAtUtc)
                .active(true)
                .build();

        banRepo.save(Objects.requireNonNull(ban));

        return BanResponse.builder()
                .targetUuid(target.getUuid())
                .reason(req.getReason())
                .durationValue(req.getDurationValue())
                .durationUnit(unit)
                .startsAtKst(nowKst)
                .expiresAtKst(expiresAtKst)
                .expiresAtUtc(expiresAtUtc)
                .build();
    }

    @Transactional
    public void releaseBanManual(Long banId) {

        requireBanPrivilege();

        UserBan ban = banRepo.findById(Objects.requireNonNull(banId, "banId is required"))
                .orElseThrow(() -> new RuntimeException("제재 내역 없음"));

        ban.setActive(false);
        banRepo.save(ban);
    }

    @Transactional
    public void releaseBan(String userUuid) {

        requireBanPrivilege();

        User target = userRepo.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        UserBan ban = banRepo.findByTargetUserAndActiveIsTrue(target)
                .orElseThrow(() -> new RuntimeException("활성 제재 없음"));

        ban.setActive(false);
        banRepo.save(ban);
    }

    @Transactional(readOnly = true)
    public BanInfoResponse getMyActiveBan() {
        User me = requireAuthenticatedUser();

        UserBan ban = banRepo.findByTargetUserAndActiveIsTrue(me).orElse(null);
        if (ban == null) {
            return null;
        }
        return toBanInfoResponse(ban);
    }

    @Transactional(readOnly = true)
    public PageResponse<BanInfoResponse> getBanList(boolean activeOnly, int page, int size) {

        requireBanPrivilege();

        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bannedAt"));

        Page<UserBan> result = activeOnly
                ? banRepo.findByActive(true, pageable)
                : banRepo.findAll(pageable);

        List<BanInfoResponse> content = result.getContent().stream()
                .map(this::toBanInfoResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                content
        );
    }

    @Transactional(readOnly = true)
    public List<BanInfoResponse> getBanHistoryForUser(String userUuid, boolean activeOnly) {

        requireBanPrivilege();

        User target = userRepo.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("제재 대상 유저를 찾을 수 없습니다."));

        List<UserBan> bans = activeOnly
                ? banRepo.findByTargetUserAndActiveOrderByBannedAtDesc(target, true)
                : banRepo.findByTargetUserOrderByBannedAtDesc(target);

        return bans.stream()
                .map(this::toBanInfoResponse)
                .collect(Collectors.toList());
    }

    private LocalDateTime calculateExpiresAt(LocalDateTime base, long value, DurationUnit unit) {
        return switch (unit) {
            case SECONDS ->
                base.plusSeconds(value);
            case MINUTES ->
                base.plusMinutes(value);
            case HOURS ->
                base.plusHours(value);
            case DAYS ->
                base.plusDays(value);
            case MONTHS ->
                base.plusMonths(value);
            case YEARS ->
                base.plusYears(value);
        };
    }

    private User requireBanPrivilege() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || (auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()))) {
            throw new AccessDeniedException("인증되지 않은 요청입니다.");
        }

        String adminUuid = auth.getName();

        User admin = userRepo.findByUuid(adminUuid)
                .orElseThrow(() -> new AccessDeniedException("관리자 계정을 찾을 수 없습니다."));

        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("제재 권한이 없습니다.");
        }

        return admin;
    }

    private User requireAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("인증 정보가 없습니다.");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof String && "anonymousUser".equals(principal)) {
            throw new AccessDeniedException("인증되지 않은 요청입니다.");
        }

        String userUuid = auth.getName();

        return userRepo.findByUuid(userUuid)
                .orElseThrow(() -> new AccessDeniedException("사용자 정보를 찾을 수 없습니다."));
    }

    private BanInfoResponse toBanInfoResponse(UserBan ban) {
        LocalDateTime bannedAtUtc = ban.getBannedAt();
        LocalDateTime expiresAtUtc = ban.getExpiresAt();

        LocalDateTime bannedAtKst = toKst(bannedAtUtc);
        LocalDateTime expiresAtKst = toKst(expiresAtUtc);

        return BanInfoResponse.builder()
                .banId(ban.getId())
                .targetUuid(ban.getTargetUser().getUuid())
                .targetNickname(ban.getTargetUser().getUserNickname())
                .adminUuid(ban.getAdminUser().getUuid())
                .adminNickname(ban.getAdminUser().getUserNickname())
                .reason(ban.getReason())
                .active(Boolean.TRUE.equals(ban.getActive()))
                .startsAtUtc(bannedAtUtc)
                .startsAtKst(bannedAtKst)
                .expiresAtUtc(expiresAtUtc)
                .expiresAtKst(expiresAtKst)
                .remainingSeconds(calculateRemainingSeconds(expiresAtUtc))
                .build();
    }

    private LocalDateTime toKst(LocalDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }
        return ZonedDateTime.of(utcTime, UTC).withZoneSameInstant(KST).toLocalDateTime();
    }

    private Long calculateRemainingSeconds(LocalDateTime expiresAtUtc) {
        if (expiresAtUtc == null) {
            return null;
        }
        long seconds = Duration.between(LocalDateTime.now(UTC), expiresAtUtc).getSeconds();
        return Math.max(seconds, 0);
    }
}
