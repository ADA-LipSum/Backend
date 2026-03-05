package com.ada.proj.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.AuthMeResponse;
import com.ada.proj.dto.AuthTokenResponse;
import com.ada.proj.dto.CreateUserRequest;
import com.ada.proj.dto.CreateUserResponse;
import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TeacherSignupRequest;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.service.AuthService;
import com.ada.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "로그인/인증")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    private final com.ada.proj.config.CookieProperties cookieProperties;

    public AuthController(
            AuthService authService,
            UserService userService,
            RefreshTokenRepository refreshTokenRepository,
            com.ada.proj.config.CookieProperties cookieProperties
    ) {
        this.authService = authService;
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cookieProperties = cookieProperties;
    }

    private ResponseCookie createCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(cookieProperties.getMaxAge())
                .build();
    }

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {

        LoginResponse res = authService.login(request);

        RefreshToken token = refreshTokenRepository.findByUuid(res.getUuid())
                .orElseThrow(() -> new RuntimeException("Refresh token not generated"));

        ResponseCookie cookie = createCookie(token.getToken());

        AuthTokenResponse body = AuthTokenResponse.builder()
                .tokenType(res.getTokenType())
                .accessToken(res.getAccessToken())
                .expiresIn(res.getExpiresIn())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(body));
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AuthMeResponse>> me(Authentication authentication) {

        AuthMeResponse res = authService.me(authentication);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> reissue(
            HttpServletRequest httpServletRequest,
            @RequestBody(required = false) TokenReissueRequest request) {

        String refreshToken = null;

        // 1) Cookie (HttpOnly refreshToken)
        refreshToken = extractRefreshTokenFromCookie(httpServletRequest);

        // 2) Authorization: Bearer <refreshToken> (compat)
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = extractBearerToken(httpServletRequest);
        }

        // 3) JSON body { refreshToken }
        if ((refreshToken == null || refreshToken.isBlank())
                && request != null
                && request.getRefreshToken() != null
                && !request.getRefreshToken().isBlank()) {
            refreshToken = request.getRefreshToken();
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.errorWithData("MISSING_REFRESH", "missing refresh token", null));
        }

        TokenReissueRequest effectiveRequest = new TokenReissueRequest();
        effectiveRequest.setRefreshToken(refreshToken);

        LoginResponse res = authService.reissue(effectiveRequest);

        RefreshToken token = refreshTokenRepository.findByUuid(res.getUuid())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        ResponseCookie cookie = createCookie(token.getToken());

        AuthTokenResponse body = AuthTokenResponse.builder()
                .tokenType(res.getTokenType())
                .accessToken(res.getAccessToken())
                .expiresIn(res.getExpiresIn())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(body));
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie != null && "refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private String extractBearerToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank()) {
            return null;
        }

        String prefix = "Bearer ";
        if (!auth.startsWith(prefix)) {
            return null;
        }

        String token = auth.substring(prefix.length()).trim();
        return token.isBlank() ? null : token;
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthenticatedException("Unauthenticated");
        }

        authService.logout(authentication.getName());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okMessage("logged out"));
    }

    @PostMapping("/logout/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "전체 로그아웃 (관리자 전용)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> globalLogout(Authentication authentication) {

        authService.globalLogout(authentication);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okMessage("all users logged out"));
    }

    @PostMapping("/signup/teacher")
    @Operation(summary = "선생님 회원가입")
    public ResponseEntity<ApiResponse<CreateUserResponse>> signupTeacher(
            @Valid @RequestBody TeacherSignupRequest req) {

        var user = authService.signupTeacher(req);
        CreateUserResponse res = new CreateUserResponse(
                user.getUuid(),
                user.getAdminId(),
                user.getCustomId(),
                user.getRole()
        );

        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자: 사용자 생성", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUserByAdmin(
            @Valid @RequestBody CreateUserRequest req,
            Authentication authentication) {

        var user = userService.createUserByAdmin(req, authentication);
        CreateUserResponse res = new CreateUserResponse(
                user.getUuid(),
                user.getAdminId(),
                user.getCustomId(),
                user.getRole()
        );

        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/admin/init")
    @Operation(summary = "초기 관리자 생성")
    public ResponseEntity<ApiResponse<CreateUserResponse>> initAdmin(
            @Valid @RequestBody CreateUserRequest req) {

        var user = userService.createInitialAdmin(req);
        CreateUserResponse res = new CreateUserResponse(
                user.getUuid(),
                user.getAdminId(),
                user.getCustomId(),
                user.getRole()
        );

        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
