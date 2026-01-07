package com.ada.proj.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ada.proj.config.CookieProperties;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.User;
import com.ada.proj.enums.Role;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import com.ada.proj.entity.SocialAccount;
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
        String name = attrs.getOrDefault("name", login).toString();
        String avatar = attrs.getOrDefault("avatar_url", "").toString();

        String adminId = "github:" + githubId;

        User user = userRepository.findByAdminId(adminId).orElse(null);
        boolean isFirstLogin = false;
        if (user == null) {
            isFirstLogin = true;
            User u = User.builder()
                    .uuid(java.util.UUID.randomUUID().toString())
                    .adminId(adminId)
                    .customId(null)
                    .password(null)
                    .userRealname(name == null || name.isBlank() ? login : name)
                    .userNickname(login == null || login.isBlank() ? name : login)
                    .profileImage(avatar)
                    .role(Role.STUDENT)
                    .build();
            user = userRepository.save(u);
        }

        // Track login
        if (!isFirstLogin && user.getLoginCount() == 0L) {
            isFirstLogin = true;
        }
        user.setLoginCount(user.getLoginCount() + 1);
        user.setLastLoginAt(Instant.now());

        // Generate tokens (similar to normal login)
        final String userUuid = user.getUuid();
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

        // Persist social account linking info so status() reflects connected providers
        try {
            String profileUrl = null;
            if (login != null && !login.isBlank()) {
                profileUrl = "https://github.com/" + login;
            }

            SocialAccount sa = socialAccountRepository.findByProviderAndProviderId("github", githubId)
                    .orElseGet(() -> SocialAccount.builder()
                    .provider("github")
                    .providerId(githubId)
                    .userUuid(userUuid)
                    .build());
            sa.setProviderLogin(login == null || login.isBlank() ? null : login);
            sa.setProviderProfileUrl(profileUrl);
            sa.setUserUuid(userUuid);
            socialAccountRepository.save(sa);
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
