package com.ada.proj.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.enums.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "소셜 로그인 (API)", description = "Swagger에서 테스트 가능한 소셜 로그인 시작/콜백 엔드포인트")
public class ApiAuthGithubController {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public ApiAuthGithubController(ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
    }

    private boolean isGithubOAuthConfigured() {
        ClientRegistrationRepository repo = clientRegistrationRepositoryProvider.getIfAvailable();
        if (repo == null) {
            return false;
        }
        try {
            return repo.findByRegistrationId("github") != null;
        } catch (Exception ignored) {
            // Be permissive if the implementation doesn't support lookup; bean presence usually implies configuration.
            return true;
        }
    }

    @GetMapping("/github")
    @Operation(summary = "GitHub 소셜 로그인 시작", description = "GitHub 인증 페이지로 302 리다이렉트합니다.")
    public ResponseEntity<?> startGithub() {
        if (!isGithubOAuthConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(ErrorCode.OAUTH_NOT_CONFIGURED.name(),
                            "GitHub OAuth2 설정이 서버에 없습니다. (prod 프로필/GT_CLIENT_ID/GT_CLIENT_SECRET 확인)"));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/oauth2/authorization/github");
        return ResponseEntity.status(302).headers(headers).build();
    }

    @GetMapping("/github/callback")
    @Operation(summary = "GitHub 소셜 로그인 콜백", description = "GitHub 인증 완료 후 호출됩니다. 성공 시 액세스 토큰(JSON)과 리프레시 토큰 쿠키를 반환합니다.")
    public ResponseEntity<ApiResponse<Void>> githubCallback() {
        // 실제 처리는 Spring Security OAuth2LoginAuthenticationFilter가 수행합니다.
        // 이 메서드는 Swagger 문서화 및 예외적인 fallback 용도입니다.
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST.name(),
                "이 콜백 URL을 직접 호출하면 400이 정상입니다. /api/auth/github 로 시작해 GitHub 로그인 후 리다이렉트로 들어오세요."));
    }
}
