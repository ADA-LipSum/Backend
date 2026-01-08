package com.ada.proj.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ada.proj.config.CookieProperties;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.SocialAccount;
import com.ada.proj.entity.User;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GitHubOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieProperties cookieProperties;
    private final com.ada.proj.repository.SocialAccountRepository socialAccountRepository;
    private final ObjectMapper objectMapper;

    public GitHubOAuth2SuccessHandler(UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            CookieProperties cookieProperties,
            com.ada.proj.repository.SocialAccountRepository socialAccountRepository,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieProperties = cookieProperties;
        this.socialAccountRepository = socialAccountRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = oauthUser.getAttributes();

        // GitHub specific: id, login, name, avatar_url, email (may be null)
        String githubId = String.valueOf(attrs.get("id"));
        String login = attrs.getOrDefault("login", "").toString();
        String avatar = attrs.getOrDefault("avatar_url", "").toString();

        // This project uses admin-issued accounts (no sign-up).
        // GitHub OAuth login is allowed only if the GitHub account is already linked.
        SocialAccount linked = socialAccountRepository.findByProviderAndProviderId("github", githubId).orElse(null);
        if (linked == null || linked.getUserUuid() == null || linked.getUserUuid().isBlank()) {
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("SOCIAL_NOT_LINKED",
                            "연동되지 않은 GitHub 계정입니다. 먼저 관리자 계정으로 로그인 후 /auth/link/github 로 연동하세요.")));
            return;
        }

        String userUuid = linked.getUserUuid();
        User user = userRepository.findByUuid(userUuid).orElse(null);
        if (user == null) {
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("LINKED_USER_NOT_FOUND",
                            "연동된 사용자 계정을 찾을 수 없습니다. (연동 데이터 확인 필요)")));
            return;
        }

        boolean isFirstLogin = user.getLoginCount() == 0L;

        // Track login
        if (!isFirstLogin && user.getLoginCount() == 0L) {
            isFirstLogin = true;
        }
        user.setLoginCount(user.getLoginCount() + 1);
        user.setLastLoginAt(Instant.now());

        // Generate tokens (similar to normal login)
        String accessToken = jwtTokenProvider.generateAccessToken(userUuid, user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userUuid, user.getRole().name());

        // Save refresh token into DB (replace existing)
        refreshTokenRepository.findByUuid(userUuid).ifPresent(rt -> refreshTokenRepository.deleteByUuid(userUuid));

        RefreshToken entity = RefreshToken.builder()
                .uuid(userUuid)
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(604800000))
                .build();
        refreshTokenRepository.save(entity);

        // Refresh social account metadata (non-fatal)
        try {
            String profileUrl = null;
            if (login != null && !login.isBlank()) {
                profileUrl = "https://github.com/" + login;
            }

            linked.setProviderLogin(login == null || login.isBlank() ? null : login);
            linked.setProviderProfileUrl(profileUrl);
            socialAccountRepository.save(linked);
        } catch (Exception ex) {
            // non-fatal: do not block authentication on socialAccount persistence failures
        }

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(cookieProperties.getMaxAge())
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Return JSON for API-style callback (Swagger-friendly)
        LoginResponse body = LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .expiresIn(900_000)
                .uuid(userUuid)
                .role(user.getRole())
                .userRealname(user.getUserRealname())
                .userNickname(user.getUserNickname())
                .profileImage(user.getProfileImage())
                .firstLogin(isFirstLogin)
                .build();

        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.ok(body)));
    }
}
