package com.ada.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.github.oauth")
public class GitHubOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope = "read:user";
    private String successRedirect;
    private String failureRedirect;
}
