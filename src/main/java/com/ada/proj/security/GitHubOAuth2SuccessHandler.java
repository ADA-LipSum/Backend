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
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.User;
import com.ada.proj.enums.Role;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GitHubOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieProperties cookieProperties;

    public GitHubOAuth2SuccessHandler(UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            CookieProperties cookieProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieProperties = cookieProperties;
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

        User user = userRepository.findByAdminId(adminId).orElseGet(() -> {
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
            return userRepository.save(u);
        });

        // Generate tokens (similar to normal login)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        // Save refresh token into DB (replace existing)
        refreshTokenRepository.findByUuid(user.getUuid()).ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
                .uuid(user.getUuid())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(604800000))
                .build();
        refreshTokenRepository.save(entity);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(cookieProperties.getMaxAge())
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Redirect to frontend (root). Adjust if you want to send accessToken via query (not recommended).
        response.sendRedirect("/");
    }
}
