package com.ada.proj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.CreateUserRequest;
import com.ada.proj.dto.CreateUserResponse;
import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TeacherSignupRequest;
import com.ada.proj.dto.TokenReissueRequest;

import com.ada.proj.entity.RefreshToken;
import com.ada.proj.repository.RefreshTokenRepository;

import com.ada.proj.service.AuthService;
import com.ada.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "로그인/인증")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthController(
            AuthService authService,
            UserService userService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.authService = authService;
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    private ResponseCookie createCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(604800)
                .build();
    }

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {

        LoginResponse res = authService.login(request);

        RefreshToken token = refreshTokenRepository.findByUuid(res.getUuid())
                .orElseThrow(() -> new RuntimeException("Refresh token not generated"));

        ResponseCookie cookie = createCookie(token.getToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(res));
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(
            @Valid @RequestBody TokenReissueRequest request) {

        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.errorWithData("MISSING_REFRESH", "missing refresh token", null));
        }

        LoginResponse res = authService.reissue(request);

        RefreshToken token = refreshTokenRepository.findByUuid(res.getUuid())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        ResponseCookie cookie = createCookie(token.getToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(res));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {

        authService.logout(authentication.getName());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okMessage("logged out"));
    }

    @PostMapping("/logout/all")
    @Operation(summary = "전체 로그아웃 (관리자 전용)")
    public ResponseEntity<ApiResponse<Void>> globalLogout(Authentication authentication) {

        authService.globalLogout(authentication);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
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
    @Operation(summary = "관리자: 사용자 생성")
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