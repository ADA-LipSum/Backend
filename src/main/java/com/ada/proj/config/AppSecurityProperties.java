package com.ada.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /**
     * HMAC secret for signing OAuth state (base64 or plain). Required for
     * /api/github/link -> /api/github/callback flow.
     */
    private String oauthStateSecret;

    /**
     * Base64-encoded 32-byte key for AES-256-GCM. Used to encrypt provider
     * access tokens at rest.
     */
    private String tokenCryptoKeyB64;
}
