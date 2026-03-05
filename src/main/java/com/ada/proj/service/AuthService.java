package com.ada.proj.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.config.JwtProperties;
import com.ada.proj.dto.AuthMeResponse;
import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TeacherSignupRequest;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.User;
import com.ada.proj.enums.Role;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.InvalidCredentialsException;
import com.ada.proj.exception.TokenExpiredException;
import com.ada.proj.exception.TokenInvalidException;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
    }

    @SuppressWarnings("null")
    public LoginResponse login(LoginRequest request) {

        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword());

        boolean isFirstLogin = user.getLoginCount() == 0L;
        user.setLoginCount(user.getLoginCount() + 1);
        user.setLastLoginAt(Instant.now());
        setDefaultAvatarIfFirstLogin(user, isFirstLogin);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        // refresh token DB 저장
        refreshTokenRepository.findByUuid(user.getUuid())
                .ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
                .uuid(user.getUuid())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()))
                .build();

        refreshTokenRepository.save(entity);

        return LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .expiresIn(jwtProperties.getAccessExpirationMs())
                .uuid(user.getUuid())
                .role(user.getRole())
                .userRealname(user.getUserRealname())
                .userNickname(user.getUserNickname())
                .profileImage(user.getProfileImage())
                .firstLogin(isFirstLogin)
                .build();
    }

    @SuppressWarnings("null")
    public LoginResponse adminLogin(LoginRequest request) {

        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword());

        if (user.getRole() != Role.ADMIN) {
            throw new InvalidCredentialsException("Invalid id or password");
        }

        boolean isFirstLogin = user.getLoginCount() == 0L;
        user.setLoginCount(user.getLoginCount() + 1);
        user.setLastLoginAt(Instant.now());
        setDefaultAvatarIfFirstLogin(user, isFirstLogin);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        refreshTokenRepository.findByUuid(user.getUuid())
                .ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .uuid(user.getUuid())
                        .token(refreshToken)
                        .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()))
                        .build()
        );

        return LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .expiresIn(jwtProperties.getAccessExpirationMs())
                .uuid(user.getUuid())
                .role(user.getRole())
                .userRealname(user.getUserRealname())
                .userNickname(user.getUserNickname())
                .profileImage(user.getProfileImage())
                .firstLogin(isFirstLogin)
                .build();
    }

    @SuppressWarnings("null")
    public LoginResponse teacherLogin(LoginRequest request) {

        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword());

        if (user.getRole() != Role.TEACHER) {
            throw new InvalidCredentialsException("Invalid id or password");
        }

        boolean isFirstLogin = user.getLoginCount() == 0L;
        user.setLoginCount(user.getLoginCount() + 1);
        user.setLastLoginAt(Instant.now());
        setDefaultAvatarIfFirstLogin(user, isFirstLogin);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        refreshTokenRepository.findByUuid(user.getUuid())
                .ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .uuid(user.getUuid())
                        .token(refreshToken)
                        .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()))
                        .build()
        );

        return LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .expiresIn(jwtProperties.getAccessExpirationMs())
                .uuid(user.getUuid())
                .role(user.getRole())
                .userRealname(user.getUserRealname())
                .userNickname(user.getUserNickname())
                .profileImage(user.getProfileImage())
                .firstLogin(isFirstLogin)
                .build();
    }

    @SuppressWarnings("null")
    public LoginResponse reissue(TokenReissueRequest request) {

        String refreshToken = request == null ? null : request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new TokenInvalidException("Invalid refresh token");
        }

        final String uuid;
        final String role;
        try {
            uuid = jwtTokenProvider.getUuid(refreshToken);
            role = jwtTokenProvider.getRole(refreshToken);
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("Refresh token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenInvalidException("Invalid refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByUuid(uuid)
                .orElseThrow(() -> new TokenInvalidException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Refresh token expired");
        }

        if (stored.getToken() == null || !stored.getToken().equals(refreshToken)) {
            throw new TokenInvalidException("Invalid refresh token");
        }

        String newAccess = jwtTokenProvider.generateAccessToken(uuid, role);
        String newRefresh = jwtTokenProvider.generateRefreshToken(uuid, role);

        stored.setToken(newRefresh);
        stored.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));
        refreshTokenRepository.save(stored);

        User user = userRepository.findByUuid(uuid).orElse(null);

        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccess)
                .expiresIn(jwtProperties.getAccessExpirationMs())
                .uuid(uuid)
                .role(role == null ? null : Role.valueOf(role));

        if (user != null) {
            builder.userRealname(user.getUserRealname())
                    .userNickname(user.getUserNickname())
                    .profileImage(user.getProfileImage())
                    .firstLogin(user.getLoginCount() == 0L);
        }

        return builder.build();
    }

    public void logout(String uuid) {
        refreshTokenRepository.deleteByUuid(uuid);
    }

    public void globalLogout(Authentication authentication) {

        if (authentication == null) {
            throw new com.ada.proj.exception.UnauthenticatedException("Unauthenticated");
        }
        if (authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(a -> a.equals("ROLE_ADMIN"))) {
            throw new ForbiddenException("Forbidden");
        }

        refreshTokenRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public AuthMeResponse me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthenticatedException("Unauthenticated");
        }

        String uuid = authentication.getName();
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isFirstLogin = user.getLoginCount() <= 1L;

        return AuthMeResponse.builder()
                .uuid(user.getUuid())
                .role(user.getRole())
                .userRealname(user.getUserRealname())
                .userNickname(user.getUserNickname())
                .profileImage(user.getProfileImage())
                .firstLogin(isFirstLogin)
                .build();
    }

    public User signupTeacher(TeacherSignupRequest req) {

        userRepository.findByAdminId(req.getTeacherId())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("teacherId exists");
                });

        if (userRepository.existsByCustomId(req.getCustomId())) {
            throw new IllegalArgumentException("customId exists");
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

        return userRepository.save(user);
    }

    private User findUserForLogin(String id) {
        return userRepository.findByAdminId(id)
                .or(() -> userRepository.findByCustomId(id))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid id or password"));
    }

    private void ensurePasswordMatchesAndUpgrade(User user, String rawPassword) {

        String stored = user.getPassword();
        String legacy = user.getLegacyCustomPw();

        if (stored == null && legacy == null) {
            throw new InvalidCredentialsException("Invalid id or password");
        }

        String candidate = stored != null ? stored : legacy;

        boolean matched;
        if (candidate.startsWith("$2a$")
                || candidate.startsWith("$2b$")
                || candidate.startsWith("$2y$")) {
            matched = passwordEncoder.matches(rawPassword, candidate);
        } else {
            matched = rawPassword.equals(candidate);
            if (matched) {
                user.setPassword(passwordEncoder.encode(candidate));
            }
        }

        if (!matched) {
            throw new InvalidCredentialsException("Invalid id or password");
        }
    }

    private void setDefaultAvatarIfFirstLogin(User user, boolean isFirstLogin) {

        if (!isFirstLogin) {
            return;
        }

        if (user.getProfileImage() != null && !user.getProfileImage().isBlank()) {
            return;
        }

        String seed = user.getUuid() == null
                ? java.util.UUID.randomUUID().toString()
                : user.getUuid().replace("-", "");
        String encodedSeed = URLEncoder.encode(seed, StandardCharsets.UTF_8);

        String url = "https://api.dicebear.com/9.x/identicon/svg?seed=" + encodedSeed;
        user.setProfileImage(url);
    }
}
