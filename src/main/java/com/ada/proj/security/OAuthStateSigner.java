package com.ada.proj.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.ada.proj.config.AppSecurityProperties;

@Component
public class OAuthStateSigner {

    private final AppSecurityProperties props;
    private final SecureRandom random = new SecureRandom();

    public OAuthStateSigner(AppSecurityProperties props) {
        this.props = props;
    }

    public String create(String userUuid, long ttlSeconds) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new IllegalArgumentException("userUuid is required");
        }
        long exp = Instant.now().getEpochSecond() + Math.max(30, ttlSeconds);
        String nonce = randomHex(12);
        String payload = userUuid + "." + exp + "." + nonce;
        String sig = sign(payload);
        return b64u(payload.getBytes(StandardCharsets.UTF_8)) + "." + sig;
    }

    public String verifyAndGetUserUuid(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("missing state");
        }
        String[] parts = state.split("\\.", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid state");
        }
        String payload = new String(b64ud(parts[0]), StandardCharsets.UTF_8);
        String sig = parts[1];
        String expected = sign(payload);
        if (!constantTimeEquals(expected, sig)) {
            throw new IllegalArgumentException("invalid state signature");
        }

        String[] fields = payload.split("\\.");
        if (fields.length != 3) {
            throw new IllegalArgumentException("invalid state payload");
        }
        String userUuid = fields[0];
        long exp;
        try {
            exp = Long.parseLong(fields[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid state exp");
        }
        if (Instant.now().getEpochSecond() > exp) {
            throw new IllegalArgumentException("state expired");
        }
        return userUuid;
    }

    private String sign(String payload) {
        String secret = props.getOauthStateSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.security.oauth-state-secret is required for GitHub linking");
        }
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return b64u(out);
        } catch (Exception e) {
            throw new IllegalStateException("failed to sign state", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= x[i] ^ y[i];
        }
        return r == 0;
    }

    private static String randomHex(int len) {
        byte[] buf = new byte[(len + 1) / 2];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder(buf.length * 2);
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.substring(0, len);
    }

    private static String b64u(byte[] v) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(v);
    }

    private static byte[] b64ud(String v) {
        return Base64.getUrlDecoder().decode(v);
    }
}
