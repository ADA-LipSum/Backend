package com.ada.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.github.oauth")
public class GitHubOAuthProperties {

    /**
     * GitHub OAuth App client id
     */
    private String clientId;

    /**
     * GitHub OAuth App client secret
     */
    private String clientSecret;

    /**
     * OAuth redirect URI registered in GitHub OAuth App
     */
    private String redirectUri;

    /**
     * Minimal scope for public profile/contributions
     */
    private String scope = "read:user";

    /**
     * Where to redirect after successful linking (front-end URL)
     */
    private String successRedirect;

    /**
     * Where to redirect after failed linking (front-end URL)
     */
    private String failureRedirect;
}
