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

@RestController
@RequestMapping("/api/github")
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

    /**
     * Start GitHub OAuth linking flow. Requires our JWT (existing account).
     */
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

    /**
     * GitHub OAuth callback for linking. This endpoint must be publicly
     * accessible.
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error,
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

    @GetMapping("/contributions")
    public ResponseEntity<?> contributions(Authentication authentication,
            @RequestParam(name = "from", required = false) String from,
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
