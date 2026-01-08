package com.ada.proj.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Optional;
import java.util.HashMap;

import org.springframework.beans.factory.ObjectProvider;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.enums.ErrorCode;
import com.ada.proj.entity.SocialAccount;
import com.ada.proj.entity.User;
import com.ada.proj.repository.SocialAccountRepository;
import com.ada.proj.repository.UserRepository;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@RestController
@RequestMapping("/auth")
@Tag(name = "소셜 계정 연동", description = "소셜 계정 연동 및 상태 확인 관련 API")
public class AuthSocialController {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public AuthSocialController(SocialAccountRepository socialAccountRepository,
            UserRepository userRepository,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
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
            return true;
        }
    }

    public static class LinkRequest {

        @Schema(description = "GitHub에서 발급받은 액세스 토큰", example = "gho_...", required = true)
        @NotBlank
        public String accessToken;
    }

    @Operation(summary = "GitHub 연동", description = "프론트엔드에서 받은 GitHub 액세스 토큰으로 현재 인증된 사용자 계정에 GitHub 계정을 연결합니다.")
    @PostMapping("/link/github")
    public ResponseEntity<ApiResponse<Void>> linkGithub(@RequestBody LinkRequest req, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.errorWithData("UNAUTH", "unauthenticated", null));
        }

        String userUuid = authentication.getName();
        RestTemplate rt = new RestTemplate();
        try {
            Map user = rt.getForObject("https://api.github.com/user?access_token={token}", Map.class, req.accessToken);
            String providerId = String.valueOf(user.get("id"));
            String login = user.getOrDefault("login", "").toString();
            String htmlUrl = user.getOrDefault("html_url", "").toString();
            // check if provider account already linked to other user
            if (socialAccountRepository.findByProviderAndProviderId("github", providerId).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.errorWithData("ALREADY_LINKED", "github account already linked", null));
            }

            socialAccountRepository.save(SocialAccount.builder()
                    .provider("github")
                    .providerId(providerId)
                    .providerLogin(login == null || login.isBlank() ? null : login)
                    .providerProfileUrl(htmlUrl == null || htmlUrl.isBlank() ? null : htmlUrl)
                    .userUuid(userUuid)
                    .build());

            return ResponseEntity.ok(ApiResponse.okMessage("linked"));
        } catch (HttpClientErrorException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.errorWithData("INVALID_TOKEN", "invalid github access token", null));
        }
    }

    @Operation(summary = "GitHub 연결 해제", description = "현재 인증된 사용자로부터 GitHub 연동 정보를 제거합니다.")
    @PostMapping("/unlink/github")
    public ResponseEntity<ApiResponse<Void>> unlinkGithub(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.errorWithData("UNAUTH", "unauthenticated", null));
        }
        String userUuid = authentication.getName();
        socialAccountRepository.deleteByUserUuidAndProvider(userUuid, "github");
        return ResponseEntity.ok(ApiResponse.okMessage("unlinked"));
    }

    @Operation(summary = "인증 상태 조회", description = "현재 인증 상태와 사용자 정보, 연동된 소셜 계정 목록을 반환합니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("authenticated", false)));
        }
        String userUuid = authentication.getName();
        List<SocialAccount> accounts = socialAccountRepository.findByUserUuid(userUuid);
        List<String> providers = accounts.stream().map(SocialAccount::getProvider).collect(Collectors.toList());

        // Build 'links' structure
        Map<String, Object> links = new HashMap<>();

        Optional<SocialAccount> githubOpt = accounts.stream().filter(a -> "github".equalsIgnoreCase(a.getProvider())).findFirst();
        boolean githubConnected = githubOpt.isPresent();
        String githubLink = null;
        if (githubConnected) {
            SocialAccount ga = githubOpt.get();
            if (ga.getProviderProfileUrl() != null) {
                githubLink = ga.getProviderProfileUrl();
            } else if (ga.getProviderLogin() != null) {
                githubLink = "https://github.com/" + ga.getProviderLogin();
            } else if (ga.getProviderId() != null) {
                githubLink = "https://github.com/" + ga.getProviderId();
            }
        }
        links.put("github", githubLink);
        links.put("githubConnected", githubConnected);

        Optional<SocialAccount> discordOpt = accounts.stream().filter(a -> "discord".equalsIgnoreCase(a.getProvider())).findFirst();
        boolean discordConnected = discordOpt.isPresent();
        String discordLink = null;
        if (discordConnected) {
            SocialAccount da = discordOpt.get();
            if (da.getProviderProfileUrl() != null) {
                discordLink = da.getProviderProfileUrl();
            } else if (da.getProviderId() != null) {
                discordLink = "https://discord.com/users/" + da.getProviderId();
            }
        }
        links.put("discord", discordLink);
        links.put("discordConnected", discordConnected);

        Optional<SocialAccount> linkedInOpt = accounts.stream().filter(a -> "linked_in".equalsIgnoreCase(a.getProvider()) || "linkedin".equalsIgnoreCase(a.getProvider())).findFirst();
        boolean linkedinConnected = linkedInOpt.isPresent();
        String linkedinLink = null;
        if (linkedinConnected) {
            SocialAccount la = linkedInOpt.get();
            if (la.getProviderProfileUrl() != null) {
                linkedinLink = la.getProviderProfileUrl();
            } else if (la.getProviderLogin() != null) {
                linkedinLink = "https://www.linkedin.com/in/" + la.getProviderLogin();
            }
        }
        links.put("linked_in", linkedinLink);
        links.put("linkedinConnected", linkedinConnected);

        User user = userRepository.findByUuid(userUuid).orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("authenticated", true);
        resp.put("uuid", userUuid);
        resp.put("user", user == null ? null : Map.of(
                "realname", user.getUserRealname(),
                "nickname", user.getUserNickname(),
                "profileImage", user.getProfileImage()
        ));
        resp.put("providers", providers);
        resp.put("links", links);

        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @GetMapping("/oauth2/redirect/github")
    public ResponseEntity<?> redirectToGitHub() {
        if (!isGithubOAuthConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(ErrorCode.OAUTH_NOT_CONFIGURED.name(),
                            "GitHub OAuth2 설정이 서버에 없습니다. (prod 프로필/GT_CLIENT_ID/GT_CLIENT_SECRET 확인)"));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/oauth2/authorization/github");
        return ResponseEntity.status(302).headers(headers).build();
    }
}
