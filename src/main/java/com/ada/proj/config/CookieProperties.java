package com.ada.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 쿠키 관련 설정을 프로퍼티로 관리합니다. - 프로덕션에서는 secure=true, sameSite=None 등으로 오버라이드하세요.
 */
@Component
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    /**
     * 쿠키를 JavaScript에서 읽을 수 없게 할지 여부 (권장: true)
     */
    private boolean httpOnly = true;

    /**
     * HTTPS 환경에서만 전송할지 여부 (prod: true)
     */
    private boolean secure = false;

    /**
     * SameSite 속성 (prod: None, local: Lax)
     */
    private String sameSite = "Lax";

    /**
     * 만료 시간(초)
     */
    private long maxAge = 604800; // 7 days

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
