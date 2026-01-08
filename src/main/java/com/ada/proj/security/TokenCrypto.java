package com.ada.proj.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.ada.proj.config.AppSecurityProperties;


@Component
public class TokenCrypto {

    private static final String VERSION = "v1";
    private static final int IV_LEN_BYTES = 12;
    private static final int TAG_LEN_BITS = 128;

    private final AppSecurityProperties props;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenCrypto(AppSecurityProperties props) {
        this.props = props;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        byte[] key = getKeyBytes();
        byte[] iv = new byte[IV_LEN_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + b64(iv) + ":" + b64(ct);
        } catch (Exception e) {
            throw new IllegalStateException("failed to encrypt token", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        String[] parts = ciphertext.split(":", 3);
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("unsupported token cipher format");
        }

        byte[] iv = b64d(parts[1]);
        byte[] ct = b64d(parts[2]);
        byte[] key = getKeyBytes();

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("failed to decrypt token", e);
        }
    }

    private byte[] getKeyBytes() {
        String keyB64 = props.getTokenCryptoKeyB64();
        if (keyB64 == null || keyB64.isBlank()) {
            throw new IllegalStateException("app.security.token-crypto-key-b64 is required to store provider tokens");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(keyB64.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.security.token-crypto-key-b64 must be base64", ex);
        }
        if (key.length != 32) {
            throw new IllegalStateException("token crypto key must be 32 bytes (AES-256)");
        }
        return key;
    }

    private static String b64(byte[] v) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(v);
    }

    private static byte[] b64d(String v) {
        return Base64.getUrlDecoder().decode(v);
    }
}
