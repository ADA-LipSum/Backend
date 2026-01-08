package com.ada.proj.service.github;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class GitHubApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public GitHubApiClient() {
    }

    public Map<String, Object> exchangeCodeForToken(String clientId, String clientSecret, String code, String redirectUri, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        if (redirectUri != null && !redirectUri.isBlank()) {
            body.add("redirect_uri", redirectUri);
        }
        if (state != null && !state.isBlank()) {
            body.add("state", state);
        }

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    "https://github.com/login/oauth/access_token",
                    new HttpEntity<>(body, headers),
                    Map.class);
            return (Map<String, Object>) resp.getBody();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("github token exchange failed: status=" + ex.getRawStatusCode(), ex);
        }
    }

    public Map<String, Object> getUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("User-Agent", "ADA-Back");

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    "https://api.github.com/user",
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
            return (Map<String, Object>) resp.getBody();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("github user fetch failed: status=" + ex.getRawStatusCode(), ex);
        }
    }

    public Map<String, Object> graphQL(String accessToken, String query, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("User-Agent", "ADA-Back");

        Map<String, Object> body = Map.of(
                "query", query,
                "variables", variables == null ? Map.of() : variables);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    "https://api.github.com/graphql",
                    new HttpEntity<>(body, headers),
                    Map.class);
            return (Map<String, Object>) resp.getBody();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("github graphql failed: status=" + ex.getRawStatusCode(), ex);
        }
    }
}
