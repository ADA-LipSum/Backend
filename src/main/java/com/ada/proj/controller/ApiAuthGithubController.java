package com.ada.proj.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "소셜 로그인 (API)", description = "Swagger에서 테스트 가능한 소셜 로그인 시작/콜백 엔드포인트")
public class ApiAuthGithubController {

    @GetMapping("/github")
    @Operation(summary = "GitHub 소셜 로그인 시작", description = "GitHub 인증 페이지로 302 리다이렉트합니다.")
    public ResponseEntity<Void> startGithub() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/oauth2/authorization/github");
        return ResponseEntity.status(302).headers(headers).build();
    }

    @GetMapping("/github/callback")
    @Operation(summary = "GitHub 소셜 로그인 콜백", description = "GitHub 인증 완료 후 호출됩니다. 성공 시 액세스 토큰(JSON)과 리프레시 토큰 쿠키를 반환합니다.")
    public ResponseEntity<Void> githubCallback() {
        // 실제 처리는 Spring Security OAuth2LoginAuthenticationFilter가 수행합니다.
        // 이 메서드는 Swagger 문서화 및 예외적인 fallback 용도입니다.
        return ResponseEntity.status(400).build();
    }
}
