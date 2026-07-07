package io.rosecloud.starter.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT configuration under {@code rosecloud.security.jwt.*}. Shared by the auth service
 * (token issuance) and the gateway (token verification), so both must use the
 * same {@code secret} and {@code issuer}.
 */
@ConfigurationProperties(prefix = "rosecloud.security.jwt")
public class JwtProperties {

    /** HMAC-SHA signing secret (UTF-8 bytes). Must be at least 32 bytes for HS256. */
    private String secret = "";

    /**
     * Allows the well-known dev-default secret to be used. Must stay {@code false}
     * in any non-development profile; the dev default is committed to the repo and
     * must never reach production. Docker Compose opts in explicitly for local runs.
     */
    private boolean allowDevSecret = true;

    /** Token issuer, verified on parse. */
    private String issuer = "rosecloud";

    /** Access token lifetime. */
    private Duration accessTtl = Duration.ofMinutes(30);

    /** Refresh token lifetime. */
    private Duration refreshTtl = Duration.ofDays(7);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isAllowDevSecret() {
        return allowDevSecret;
    }

    public void setAllowDevSecret(boolean allowDevSecret) {
        this.allowDevSecret = allowDevSecret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getAccessTtl() {
        return accessTtl;
    }

    public void setAccessTtl(Duration accessTtl) {
        this.accessTtl = accessTtl;
    }

    public Duration getRefreshTtl() {
        return refreshTtl;
    }

    public void setRefreshTtl(Duration refreshTtl) {
        this.refreshTtl = refreshTtl;
    }
}
