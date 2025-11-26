package com.ada.proj.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TeacherSignupRequest;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.InvalidCredentialsException;
import com.ada.proj.exception.TokenExpiredException;
import com.ada.proj.exception.TokenInvalidException;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;

@Service
@Transactional
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        if (log.isInfoEnabled()) {
            log.info("[AUTH] login attempt id={}", safeId(request.getId()));
        }
        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword(), request.getId());

    // 첫 로그인 여부 판단 및 업데이트
    boolean isFirstLogin = (user.getLoginCount() == 0L);
    user.setLoginCount(user.getLoginCount() + 1);
    user.setLastLoginAt(Instant.now());
    // 첫 로그인 시 프로필 이미지 기본값(identicon) 설정
    setDefaultAvatarIfFirstLogin(user, isFirstLogin);

    String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        // 기존 refresh 삭제 후 저장(1인 1개 정책)
        refreshTokenRepository.findByUuid(user.getUuid()).ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
            .uuid(user.getUuid())
            .token(refreshToken)
            .expiresAt(Instant.now().plusMillis(604800000)) // default 7d (동일 설정)
            .build();
        refreshTokenRepository.save(Objects.requireNonNull(entity));

    LoginResponse resp = LoginResponse.builder()
        .tokenType("Bearer")
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresIn(900_000)
        .uuid(user.getUuid())
        .role(user.getRole())
        .userRealname(user.getUserRealname())
        .userNickname(user.getUserNickname())
        .profileImage(user.getProfileImage())
        .firstLogin(isFirstLogin)
        .build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] login success uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
        }
        return resp;
    }

    public LoginResponse adminLogin(LoginRequest request) {
        if (log.isInfoEnabled()) {
            log.info("[AUTH] admin login attempt id={}", safeId(request.getId()));
        }
        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword(), request.getId());

        // 관리자 전용 체크
        if (user.getRole() != Role.ADMIN) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] admin login rejected: not admin uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
            }
            // 일반 메시지로 응답(보안상 상세 노출 회피)
            throw new InvalidCredentialsException("Invalid id or password");
        }

    boolean isFirstLogin = (user.getLoginCount() == 0L);
    user.setLoginCount(user.getLoginCount() + 1);
    user.setLastLoginAt(Instant.now());
    // 첫 로그인 시 프로필 이미지 기본값(identicon) 설정
    setDefaultAvatarIfFirstLogin(user, isFirstLogin);

    String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        refreshTokenRepository.findByUuid(user.getUuid()).ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
            .uuid(user.getUuid())
            .token(refreshToken)
            .expiresAt(Instant.now().plusMillis(604800000))
            .build();
        refreshTokenRepository.save(Objects.requireNonNull(entity));

    LoginResponse resp = LoginResponse.builder()
        .tokenType("Bearer")
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresIn(900_000)
        .uuid(user.getUuid())
        .role(user.getRole())
        .userRealname(user.getUserRealname())
        .userNickname(user.getUserNickname())
        .profileImage(user.getProfileImage())
        .firstLogin(isFirstLogin)
        .build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] admin login success uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
        }
        return resp;
    }

    public LoginResponse teacherLogin(LoginRequest request) {
        if (log.isInfoEnabled()) {
            log.info("[AUTH] teacher login attempt id={}", safeId(request.getId()));
        }
        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword(), request.getId());

        // 선생님 전용 체크
        if (user.getRole() != Role.TEACHER) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] teacher login rejected: not teacher uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
            }
            throw new InvalidCredentialsException("Invalid id or password");
        }

    boolean isFirstLogin = (user.getLoginCount() == 0L);
    user.setLoginCount(user.getLoginCount() + 1);
    user.setLastLoginAt(Instant.now());
    // 첫 로그인 시 프로필 이미지 기본값(identicon) 설정
    setDefaultAvatarIfFirstLogin(user, isFirstLogin);

    String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        refreshTokenRepository.findByUuid(user.getUuid()).ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
            .uuid(user.getUuid())
            .token(refreshToken)
            .expiresAt(Instant.now().plusMillis(604800000))
            .build();
        refreshTokenRepository.save(Objects.requireNonNull(entity));

    LoginResponse resp = LoginResponse.builder()
        .tokenType("Bearer")
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresIn(900_000)
        .uuid(user.getUuid())
        .role(user.getRole())
        .userRealname(user.getUserRealname())
        .userNickname(user.getUserNickname())
        .profileImage(user.getProfileImage())
        .firstLogin(isFirstLogin)
        .build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] teacher login success uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
        }
        return resp;
    }

    /**
     * Swagger UI 등 개발 편의를 위한 관리자 전용 단축 로그인입니다.
     * 하드코딩된 아이디/비밀번호로 접근 시 ADMIN 권한의 토큰을 발급합니다.
     */
    public LoginResponse swaggerLogin(LoginRequest request) {
        final String SW_ID = "admin";
        final String SW_PW = "adminadmin1234";
        if (request == null || request.getId() == null || request.getPassword() == null
                || !SW_ID.equals(request.getId()) || !SW_PW.equals(request.getPassword())) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] swagger login rejected id={}", safeId(request == null ? null : request.getId()));
            }
            throw new com.ada.proj.exception.InvalidCredentialsException("Invalid id or password");
        }

        // 고정 UUID 사용(반복 호출 시 이전 refresh 토큰을 덮어쓰기 위해 동일 uuid 사용)
        String uuid = "00000000-0000-0000-0000-000000000000";
        boolean isFirstLogin = false;

        String accessToken = jwtTokenProvider.generateAccessToken(uuid, Role.ADMIN.name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(uuid, Role.ADMIN.name());

        // 기존 refresh 삭제 후 저장(1인 1개 정책)
        refreshTokenRepository.findByUuid(uuid).ifPresent(rt -> refreshTokenRepository.deleteByUuid(uuid));

        RefreshToken entity = RefreshToken.builder()
            .uuid(uuid)
            .token(refreshToken)
            .expiresAt(Instant.now().plusMillis(604800000))
            .build();
        refreshTokenRepository.save(Objects.requireNonNull(entity));

        LoginResponse resp = LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900_000)
                .uuid(uuid)
                .role(Role.ADMIN)
                .userRealname("Swagger Admin")
                .userNickname("swagger")
                .profileImage(null)
                .firstLogin(isFirstLogin)
                .build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] swagger login success uuid={} role={}", safeUuid(uuid), Role.ADMIN);
        }
        return resp;
    }

    private User findUserForLogin(String id) {
        return userRepository.findByAdminId(id)
                .or(() -> userRepository.findByCustomId(id))
                .orElseThrow(() -> {
                    if (log.isWarnEnabled()) {
                        log.warn("[AUTH] login failed: user not found id={}", safeId(id));
                    }
                    return new InvalidCredentialsException("Invalid id or password");
                });
    }

    private void ensurePasswordMatchesAndUpgrade(User user, String rawPassword, String idForLog) {
        String stored = user.getPassword();
        String legacy = user.getLegacyCustomPw();
        if (stored == null && legacy == null) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] login failed: no credential on record id={}", safeId(idForLog));
            }
            throw new InvalidCredentialsException("Invalid id or password");
        }

        String candidate = stored != null ? stored : legacy;
        boolean matched;
        if (candidate.startsWith("$2a$") || candidate.startsWith("$2b$") || candidate.startsWith("$2y$")) {
            matched = passwordEncoder.matches(rawPassword, candidate);
        } else {
            matched = rawPassword.equals(candidate);
            if (matched) {
                user.setPassword(passwordEncoder.encode(candidate));
            }
        }

        if (matched && user.getPassword() == null && user.getLegacyCustomPw() != null) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        if (!matched) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] login failed: bad password id={}", safeId(idForLog));
            }
            throw new IllegalArgumentException("Invalid id or password");
        }
    }

    public LoginResponse reissue(TokenReissueRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    if (log.isWarnEnabled()) {
                        log.warn("[AUTH] refresh failed: token not found");
                    }
                    return new TokenInvalidException("Invalid refresh token");
                });

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] refresh failed: token expired uuid={} ", safeUuid(stored.getUuid()));
            }
            throw new TokenExpiredException("Refresh token expired");
        }

        String uuid = jwtTokenProvider.getUuid(stored.getToken());
        String role = jwtTokenProvider.getRole(stored.getToken());

        String newAccess = jwtTokenProvider.generateAccessToken(uuid, role);
        String newRefresh = jwtTokenProvider.generateRefreshToken(uuid, role);

        stored.setToken(newRefresh);
        stored.setExpiresAt(Instant.now().plusMillis(604800000));

    // uuid / role 로 사용자 조회하여 프로필 포함 (토큰 재발급에도 일관된 응답)
    User user = userRepository.findByUuid(uuid)
        .orElse(null); // 없어도 토큰은 재발급 가능하지만 일반적으로 존재해야 함

    LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
        .tokenType("Bearer")
        .accessToken(newAccess)
        .refreshToken(newRefresh)
        .expiresIn(900_000)
        .uuid(uuid)
        .role(role == null ? null : com.ada.proj.entity.Role.valueOf(role));

    if (user != null) {
        builder.userRealname(user.getUserRealname())
           .userNickname(user.getUserNickname())
           .profileImage(user.getProfileImage())
           .firstLogin(user.getLoginCount() == 0L);
    }
    LoginResponse resp = builder.build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] refresh success uuid={} ", safeUuid(uuid));
        }
        return resp;
    }

    public void logout(String uuid) {
        refreshTokenRepository.deleteByUuid(uuid);
        if (log.isInfoEnabled()) {
            log.info("[AUTH] logout uuid={}", safeUuid(uuid));
        }
    }

    /**
     * 관리자 전용: 모든 사용자의 refresh 토큰을 일괄 폐기 (사실상 전체 로그아웃)
     */
    public void globalLogout(Authentication authentication) {
        if (authentication == null) {
            throw new com.ada.proj.exception.UnauthenticatedException("Unauthenticated");
        }
        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).noneMatch(a -> a.equals("ROLE_ADMIN"))) {
            throw new ForbiddenException("Forbidden");
        }
        refreshTokenRepository.deleteAll();
        if (log.isWarnEnabled()) {
            log.warn("[AUTH] global logout executed by admin uuid={}", authentication.getName());
        }
    }

    /**
     * 선생님 자가 회원가입
     */
    public User signupTeacher(TeacherSignupRequest req) {
        // teacherId 를 adminId 로 사용
        userRepository.findByAdminId(req.getTeacherId()).ifPresent(u -> {
            throw new IllegalArgumentException("teacherId already exists");
        });
        if (userRepository.existsByCustomId(req.getCustomId())) {
            throw new IllegalArgumentException("customId already exists");
        }

        User user = User.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .adminId(req.getTeacherId())
                .customId(req.getCustomId())
                .password(passwordEncoder.encode(req.getPassword()))
                .userRealname(req.getUserRealname())
                .userNickname(req.getUserNickname())
                .role(Role.TEACHER)
                .build();
        return userRepository.save(Objects.requireNonNull(user));
    }

    private String safeId(String id) {
        if (id == null) return "";
        if (id.length() <= 2) return id;
        return id.substring(0, Math.min(2, id.length())) + "***";
    }

    private String safeUuid(String uuid) {
        if (uuid == null || uuid.length() < 4) return String.valueOf(uuid);
        return "****" + uuid.substring(uuid.length() - 4);
    }
    /**
     * 첫 로그인 시 기본 identicon 아바타를 설정합니다.
     * seed 재사용을 방지하기 위해 사용자 UUID를 시드로 사용합니다(전역 유일).
     * 이미 프로필 이미지가 있는 경우 건드리지 않습니다.
     */
    private void setDefaultAvatarIfFirstLogin(User user, boolean isFirstLogin) {
        if (!isFirstLogin) return;
        String current = user.getProfileImage();
        if (current != null && !current.isBlank()) return;
        // DiceBear identicon URL 생성 (seed는 사용자 UUID 사용: 전역 유일, 재사용 방지)
        String seed = user.getUuid() == null ? java.util.UUID.randomUUID().toString() : user.getUuid().replace("-", "");
        String encodedSeed = URLEncoder.encode(seed, StandardCharsets.UTF_8);
        String url = "https://api.dicebear.com/9.x/identicon/svg?seed=" + encodedSeed;
        user.setProfileImage(url);
    }
}
