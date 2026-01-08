package com.ada.proj.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.config.GitHubOAuthProperties;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.entity.SocialAccount;
import com.ada.proj.repository.SocialAccountRepository;
import com.ada.proj.security.OAuthStateSigner;
import com.ada.proj.security.TokenCrypto;
import com.ada.proj.service.github.GitHubApiClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/github")
@Tag(name = "GitHub 연동", description = "기존 계정에 GitHub를 연동하고, 연동된 토큰으로 contribution 데이터를 조회하는 API")
public class GitHubLinkController {

    private final GitHubOAuthProperties gitHubOAuthProperties;
    private final OAuthStateSigner stateSigner;
    private final GitHubApiClient gitHubApiClient;
    private final SocialAccountRepository socialAccountRepository;
    private final TokenCrypto tokenCrypto;

    public GitHubLinkController(GitHubOAuthProperties gitHubOAuthProperties,
            OAuthStateSigner stateSigner,
            GitHubApiClient gitHubApiClient,
            SocialAccountRepository socialAccountRepository,
            TokenCrypto tokenCrypto) {
        this.gitHubOAuthProperties = gitHubOAuthProperties;
        this.stateSigner = stateSigner;
        this.gitHubApiClient = gitHubApiClient;
        this.socialAccountRepository = socialAccountRepository;
        this.tokenCrypto = tokenCrypto;
    }

    @Operation(
            summary = "GitHub 연동 시작(Authorize로 리다이렉트)",
            description = "이미 로그인된(관리자 발급 계정) 사용자만 GitHub를 연동할 수 있습니다.\n"
            + "이 엔드포인트는 GitHub OAuth Authorize URL로 302 리다이렉트합니다.\n"
            + "프론트에서 직접 GitHub 토큰을 받지 않으며, 콜백(/api/github/callback)에서 백엔드가 code -> token 교환을 수행합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "GitHub 로그인/동의 화면으로 이동 (Location 헤더)", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요(JWT 누락/만료)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 설정 누락(client-id/redirect-uri 등)", content = @Content)
    })
    @GetMapping("/link")
    public ResponseEntity<?> link(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTH", "unauthenticated"));
        }
        String userUuid = authentication.getName();

        String clientId = required(gitHubOAuthProperties.getClientId(), "app.github.oauth.client-id");
        String redirectUri = required(gitHubOAuthProperties.getRedirectUri(), "app.github.oauth.redirect-uri");
        String scope = gitHubOAuthProperties.getScope();
        if (scope == null || scope.isBlank()) {
            scope = "read:user";
        }

        String state = stateSigner.create(userUuid, 300);
        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&scope=" + enc(scope)
                + "&state=" + enc(state);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", url);
        return ResponseEntity.status(302).headers(headers).build();
    }

    @Operation(
            summary = "GitHub 연동 콜백(code/state 처리)",
            description = "GitHub OAuth authorize 완료 후 GitHub가 redirect하는 콜백 엔드포인트입니다.\n"
            + "브라우저가 이 URL로 들어오며, 서버는 code를 access token으로 교환하고(백엔드에서만 처리),\n"
            + "연동 정보를 DB에 저장한 뒤 성공/실패 redirect URL로 다시 302 이동합니다.\n"
            + "주의: Swagger에서 직접 호출하지 않고, /api/github/link 플로우로만 진입합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "성공/실패 redirect (Location 헤더)", content = @Content)
    })
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @Parameter(description = "GitHub가 발급한 Authorization Code", example = "0a1b2c3d4e5f6g7h")
            @RequestParam(name = "code", required = false) String code,
            @Parameter(description = "CSRF 방지용 state(서명 포함). /api/github/link가 생성", example = "<signed-state>")
            @RequestParam(name = "state", required = false) String state,
            @Parameter(description = "GitHub 에러 코드(사용자가 동의 취소 등)", example = "access_denied")
            @RequestParam(name = "error", required = false) String error,
            @Parameter(description = "GitHub 에러 상세 설명", example = "The user denied the request")
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        String successRedirect = gitHubOAuthProperties.getSuccessRedirect();
        String failureRedirect = gitHubOAuthProperties.getFailureRedirect();

        if (error != null && !error.isBlank()) {
            return redirect(failureRedirect, "error=" + enc(error) + "&desc=" + enc(nullToEmpty(errorDescription)));
        }
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return redirect(failureRedirect, "error=missing_code_or_state");
        }

        final String userUuid;
        try {
            userUuid = stateSigner.verifyAndGetUserUuid(state);
        } catch (Exception ex) {
            return redirect(failureRedirect, "error=invalid_state");
        }

        String clientId = required(gitHubOAuthProperties.getClientId(), "app.github.oauth.client-id");
        String clientSecret = required(gitHubOAuthProperties.getClientSecret(), "app.github.oauth.client-secret");
        String redirectUri = required(gitHubOAuthProperties.getRedirectUri(), "app.github.oauth.redirect-uri");

        Map<String, Object> tokenResp = gitHubApiClient.exchangeCodeForToken(clientId, clientSecret, code, redirectUri, state);
        String accessToken = tokenResp == null ? null : String.valueOf(tokenResp.get("access_token"));
        String scope = tokenResp == null || tokenResp.get("scope") == null ? null : String.valueOf(tokenResp.get("scope"));

        if (accessToken == null || accessToken.isBlank() || "null".equals(accessToken)) {
            return redirect(failureRedirect, "error=token_exchange_failed");
        }

        Map<String, Object> user = gitHubApiClient.getUser(accessToken);
        String githubId = user == null ? null : String.valueOf(user.get("id"));
        String login = user == null || user.get("login") == null ? null : String.valueOf(user.get("login"));
        String htmlUrl = user == null || user.get("html_url") == null ? null : String.valueOf(user.get("html_url"));

        if (githubId == null || githubId.isBlank() || "null".equals(githubId)) {
            return redirect(failureRedirect, "error=github_user_missing_id");
        }

        SocialAccount existingByProvider = socialAccountRepository.findByProviderAndProviderId("github", githubId).orElse(null);
        if (existingByProvider != null && existingByProvider.getUserUuid() != null
                && !existingByProvider.getUserUuid().equals(userUuid)) {
            return redirect(failureRedirect, "error=already_linked");
        }

        SocialAccount account = existingByProvider != null ? existingByProvider : new SocialAccount();
        account.setProvider("github");
        account.setProviderId(githubId);
        account.setProviderLogin((login == null || login.isBlank()) ? null : login);
        account.setProviderProfileUrl((htmlUrl == null || htmlUrl.isBlank()) ? null : htmlUrl);
        account.setUserUuid(userUuid);
        account.setAccessTokenEnc(tokenCrypto.encrypt(accessToken));
        account.setTokenScope(scope);
        account.setConnectedAt(Instant.now());
        socialAccountRepository.save(account);

        return redirect(successRedirect, "linked=github");
    }

    @Operation(
            summary = "GitHub Contribution 그래프 데이터 조회",
            description = "연동된 GitHub 계정의 contribution 데이터를 조회해 프론트가 그릴 수 있는 형태로 반환합니다.\n"
            + "토큰은 프론트에 노출하지 않으며, DB에 암호화 저장된 토큰을 복호화하여 GitHub GraphQL(contributionsCollection)을 호출합니다.\n"
            + "from/to 미지정 시 기본값은 to=오늘(UTC), from=to-365일 입니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "GitHub 미연동/토큰 누락/로그인(login) 누락", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요(JWT 누락/만료)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "GitHub GraphQL 응답 파싱 실패/일시 오류", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/contributions")
    public ResponseEntity<?> contributions(Authentication authentication,
            @Parameter(description = "조회 시작일(YYYY-MM-DD). 미지정 시 to-365일", example = "2025-01-01")
            @RequestParam(name = "from", required = false) String from,
            @Parameter(description = "조회 종료일(YYYY-MM-DD). 미지정 시 오늘(UTC)", example = "2026-01-01")
            @RequestParam(name = "to", required = false) String to) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTH", "unauthenticated"));
        }
        String userUuid = authentication.getName();

        SocialAccount account = socialAccountRepository.findByUserUuidAndProvider(userUuid, "github").orElse(null);
        if (account == null || account.getAccessTokenEnc() == null || account.getAccessTokenEnc().isBlank()) {
            return ResponseEntity.status(400).body(ApiResponse.error("GITHUB_NOT_LINKED", "github not linked"));
        }

        String token = tokenCrypto.decrypt(account.getAccessTokenEnc());
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(400).body(ApiResponse.error("GITHUB_TOKEN_MISSING", "github token missing"));
        }

        String login = account.getProviderLogin();
        if (login == null || login.isBlank()) {
            Map<String, Object> me = gitHubApiClient.getUser(token);
            login = me == null || me.get("login") == null ? null : String.valueOf(me.get("login"));
        }
        if (login == null || login.isBlank()) {
            return ResponseEntity.status(400).body(ApiResponse.error("GITHUB_LOGIN_MISSING", "github login missing"));
        }

        Instant now = Instant.now();
        LocalDate toDate = parseDateOrDefault(to, LocalDate.ofInstant(now, ZoneOffset.UTC));
        LocalDate fromDate = parseDateOrDefault(from, toDate.minusDays(365));

        String query = "query($login:String!, $from:DateTime!, $to:DateTime!){"
                + " user(login:$login){"
                + "  contributionsCollection(from:$from,to:$to){"
                + "   contributionCalendar{ totalContributions weeks{ contributionDays{ date contributionCount } } }"
                + "  }"
                + " }"
                + "}";

        Map<String, Object> vars = Map.of(
                "login", login,
                "from", fromDate.atStartOfDay().atOffset(ZoneOffset.UTC).toString(),
                "to", toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).toString());

        Map<String, Object> gql = gitHubApiClient.graphQL(token, query, vars);
        Map<String, Object> data = gql == null ? null : (Map<String, Object>) gql.get("data");
        Map<String, Object> userNode = data == null ? null : (Map<String, Object>) data.get("user");
        Map<String, Object> cc = userNode == null ? null : (Map<String, Object>) userNode.get("contributionsCollection");
        Map<String, Object> cal = cc == null ? null : (Map<String, Object>) cc.get("contributionCalendar");

        if (cal == null) {
            return ResponseEntity.status(502).body(ApiResponse.error("GITHUB_GRAPHQL_FAILED", "failed to read contributions"));
        }

        Number total = (Number) cal.get("totalContributions");
        List<Map<String, Object>> weeks = (List<Map<String, Object>>) cal.get("weeks");
        List<Map<String, Object>> days = new ArrayList<>();
        if (weeks != null) {
            for (Map<String, Object> w : weeks) {
                List<Map<String, Object>> d = (List<Map<String, Object>>) w.get("contributionDays");
                if (d != null) {
                    for (Map<String, Object> day : d) {
                        days.add(Map.of(
                                "date", day.get("date"),
                                "count", day.get("contributionCount")));
                    }
                }
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("login", login);
        resp.put("from", fromDate.toString());
        resp.put("to", toDate.toString());
        resp.put("totalContributions", total == null ? 0 : total.intValue());
        resp.put("days", days);

        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    private static ResponseEntity<?> redirect(String base, String query) {
        String target = (base == null || base.isBlank()) ? "/" : base;
        if (query != null && !query.isBlank()) {
            target = target.contains("?") ? (target + "&" + query) : (target + "?" + query);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", target);
        return ResponseEntity.status(302).headers(headers).build();
    }

    private static String required(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return v;
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static LocalDate parseDateOrDefault(String v, LocalDate def) {
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return LocalDate.parse(v);
        } catch (Exception e) {
            return def;
        }
    }
}
