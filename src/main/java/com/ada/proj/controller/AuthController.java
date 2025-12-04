package com.ada.proj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.CreateUserRequest;
import com.ada.proj.dto.CreateUserResponse;
import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TeacherSignupRequest;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.service.AuthService;
import com.ada.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "로그인/인증", description = "로그인, 토큰 재발급, 로그아웃 등 인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "관리자 발급 ID/PW 또는 커스텀 ID/PW로 로그인")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Parameter(description = "로그인 요청 바디")
            @Valid @RequestBody LoginRequest request) {
        LoginResponse res = authService.login(request);
        // refreshToken 을 HttpOnly 쿠키와 응답 바디 모두로 전달
        String refresh = res.getRefreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(604800) // 7 days
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(res));
    }

    @PostMapping("/swagger-login")
    @Operation(summary = "Swagger용 단축 로그인", description = "개발 편의를 위한 단축 로그인: id=admin, pw=adminadmin1234")
    public ResponseEntity<ApiResponse<LoginResponse>> swaggerLogin(
            @Parameter(description = "로그인 요청 바디")
            @Valid @RequestBody LoginRequest request) {
        LoginResponse res = authService.swaggerLogin(request);
        String refresh = res.getRefreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(604800)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(res));
    }

    @GetMapping("/swagger-login")
    @Operation(summary = "Swagger용 자동 로그인(GET)", description = "개발 편의를 위한 자동 로그인 (id/pw 입력 불필요)")
    public ResponseEntity<ApiResponse<LoginResponse>> swaggerLoginGet() {
        LoginRequest req = new LoginRequest();
        req.setId("admin");
        req.setPassword("adminadmin1234");
        LoginResponse res = authService.swaggerLogin(req);
        String refresh = res.getRefreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(604800)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(res));
    }

    @Deprecated
    @PostMapping("/admin/login")
    @Operation(summary = "[Deprecated] 관리자 전용 로그인", description = "통합 로그인으로 대체됨. /auth/login 사용. 응답의 role 로 분기")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(
            @Parameter(description = "로그인 요청 바디")
            @Valid @RequestBody LoginRequest request) {
        LoginResponse res = authService.login(request);
        String refresh = res.getRefreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(604800)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(res));
    }

    @Deprecated
    @PostMapping("/teacher/login")
    @Operation(summary = "[Deprecated] 선생님 전용 로그인", description = "통합 로그인으로 대체됨. /auth/login 사용. 응답의 role 로 분기")
    public ResponseEntity<ApiResponse<LoginResponse>> teacherLogin(
            @Parameter(description = "로그인 요청 바디")
            @Valid @RequestBody LoginRequest request) {
        LoginResponse res = authService.login(request);
        String refresh = res.getRefreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(604800)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(res));
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access/Refresh 재발급")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(
            @Parameter(description = "토큰 재발급: 쿠키(refreshToken) 우선, 없으면 바디 사용")
            @CookieValue(value = "refreshToken", required = false) String refreshCookie,
            @Valid @RequestBody(required = false) TokenReissueRequest request) {
        String token = refreshCookie != null && !refreshCookie.isBlank() ? refreshCookie : (request == null ? null : request.getRefreshToken());
        if (token == null) {
            return ResponseEntity.badRequest().body(ApiResponse.errorWithData("MISSING_REFRESH", "missing refresh token", null));
        }
        TokenReissueRequest req = new TokenReissueRequest();
        req.setRefreshToken(token);
        LoginResponse res = authService.reissue(req);
        String refresh = res.getRefreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(604800)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(res));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token 폐기")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        String uuid = authentication.getName();
        authService.logout(uuid);
        // 쿠키 제거
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okMessage("logged out"));
    }

    @PostMapping("/logout/all")
    @Operation(summary = "전체 로그아웃", description = "역할 무관 전체 사용자 로그아웃 (관리자 전용). 모든 refresh 토큰 폐기")
    public ResponseEntity<ApiResponse<Void>> globalLogout(Authentication authentication) {
        // 관리자 권한 체크는 서비스 내부 혹은 여기서 수행
        authService.globalLogout(authentication);
        // 모든 사용자 토큰 삭제 + 클라이언트 쿠키 제거
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okMessage("all users logged out"));
    }

    @PostMapping("/signup/teacher")
    @Operation(summary = "선생님 회원가입", description = "선생님 역할 계정을 직접 생성")
    public ResponseEntity<ApiResponse<CreateUserResponse>> signupTeacher(
            @Parameter(description = "선생님 회원가입 요청 바디")
            @Valid @RequestBody TeacherSignupRequest req) {
        var user = authService.signupTeacher(req);
        CreateUserResponse res = new CreateUserResponse(user.getUuid(), user.getAdminId(), user.getCustomId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/admin/create")
    @Operation(summary = "관리자: 사용자 생성", description = "관리자가 새로운 사용자 계정을 생성합니다")
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUserByAdmin(
            @Parameter(description = "관리자 사용자 생성 요청 바디")
            @Valid @RequestBody CreateUserRequest req, Authentication authentication) {
        var user = userService.createUserByAdmin(req, authentication);
        CreateUserResponse res = new CreateUserResponse(user.getUuid(), user.getAdminId(), user.getCustomId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/admin/init")
    @Operation(summary = "초기 관리자 생성", description = "시스템에 ADMIN이 하나도 없을 때 최초의 ADMIN 계정을 생성합니다")
    public ResponseEntity<ApiResponse<CreateUserResponse>> initAdmin(
            @Parameter(description = "최초 관리자 생성 요청 바디")
            @Valid @RequestBody CreateUserRequest req) {
        var user = userService.createInitialAdmin(req);
        CreateUserResponse res = new CreateUserResponse(user.getUuid(), user.getAdminId(), user.getCustomId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
