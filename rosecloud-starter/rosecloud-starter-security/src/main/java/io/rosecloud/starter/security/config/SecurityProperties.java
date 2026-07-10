package io.rosecloud.starter.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

import static io.rosecloud.common.core.model.ServiceMetadata.API_PREFIX;

@ConfigurationProperties(prefix = "rosecloud.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private long accessTokenExpirationSeconds = 3600;
    private long refreshTokenExpirationSeconds = 86400;
    // M4: `/error` is intentionally NOT in the default public list. Spring Boot forwards
    // unauthenticated errors to `/error`; leaving it permitAll can leak error bodies before
    // authentication. With it authenticated, such requests get a normal 401 instead.
    // `/actuator/health/**` stays permitAll for infra probes but MUST sit behind a network
    // policy (do not expose the actuator publicly). Override via `rosecloud.security.public-paths`.
    private String[] publicPaths = {API_PREFIX + "/auth/login", API_PREFIX + "/auth/refresh", API_PREFIX + "/auth/logout",
            API_PREFIX + "/noauth/**", API_PREFIX + "/public/**", "/actuator/health/**"};
    private Cors cors = new Cors();
    // L3: single static shared secret for internal (machine-to-machine) calls. Constant-time
    // comparison is used on the verify side. There is no rotation and no per-service credential;
    // acceptable given the gateway strips inbound X-Internal, but guard the leaked-secret blast
    // radius and never log this value.
    private String internalToken;
    private BruteForce bruteForce = new BruteForce();
    private TokenBinding tokenBinding = new TokenBinding();
    private TenantWriteGuard tenantWriteGuard = new TenantWriteGuard();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }

    public String getInternalToken() { return internalToken; }
    public void setInternalToken(String internalToken) { this.internalToken = internalToken; }

    public BruteForce getBruteForce() { return bruteForce; }
    public void setBruteForce(BruteForce bruteForce) { this.bruteForce = bruteForce; }

    public TokenBinding getTokenBinding() { return tokenBinding; }
    public void setTokenBinding(TokenBinding tokenBinding) { this.tokenBinding = tokenBinding; }

    public TenantWriteGuard getTenantWriteGuard() { return tenantWriteGuard; }
    public void setTenantWriteGuard(TenantWriteGuard tenantWriteGuard) { this.tenantWriteGuard = tenantWriteGuard; }

    public long getAccessTokenExpirationSeconds() { return accessTokenExpirationSeconds; }
    public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) { this.accessTokenExpirationSeconds = accessTokenExpirationSeconds; }

    public long getRefreshTokenExpirationSeconds() { return refreshTokenExpirationSeconds; }
    public void setRefreshTokenExpirationSeconds(long refreshTokenExpirationSeconds) { this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds; }

    public String[] getPublicPaths() { return publicPaths; }
    public void setPublicPaths(String[] publicPaths) { this.publicPaths = publicPaths; }

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }

    /**
     * CORS settings. Origins must be listed explicitly — a wildcard {@code *} is never
     * combined with credentials, which the browser rejects and which is unsafe. When no
     * origins are configured, cross-origin browser requests are simply not permitted.
     */
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        private List<String> allowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type", "X-Requested-With"));
        private boolean allowCredentials = true;
        private long maxAge = 3600;

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }

        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }

        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }

        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }

        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
    }

    public static class Jwt {
        private String secret;
        private String issuer = "rosecloud";
        // M2: audience claim. Tokens are minted with this `aud` and the parser requires it,
        // so a token issued for one audience cannot be replayed against a service expecting
        // a different one. Defaults to the shared issuer value.
        private String audience = "rosecloud";

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }

        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
    }

    /**
     * H3: brute-force / credential-stuffing protection for the login and refresh endpoints.
     * Counts consecutive failures per account; once {@link #maxFailedAttempts} is reached the
     * account is locked for {@link #lockoutDurationSeconds}. Backed by Redis when available;
     * without Redis the protection is a no-op (and a warning is logged).
     */
    public static class BruteForce {
        private boolean enabled = true;
        private int maxFailedAttempts = 5;
        private long lockoutDurationSeconds = 900;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }

        public long getLockoutDurationSeconds() { return lockoutDurationSeconds; }
        public void setLockoutDurationSeconds(long lockoutDurationSeconds) { this.lockoutDurationSeconds = lockoutDurationSeconds; }
    }

    /**
     * M3: optional device binding. When enabled, a hash of the client IP + User-Agent is
     * embedded in the token at mint time and verified on every request, so a stolen bearer
     * token cannot be replayed from a different device. Tokens minted with binding carry an
     * `fp` claim and are always verified regardless of this flag (it only controls minting).
     */
    public static class TokenBinding {
        private boolean enabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * M5: tenant write-guard strictness. When {@link #failClosed} is true and the tenant
     * status cannot be resolved (no user API, or the lookup failed), mutating requests are
     * blocked instead of allowed. Default false preserves the original fail-open behaviour
     * for services that legitimately have no user API.
     */
    public static class TenantWriteGuard {
        private boolean failClosed = false;

        public boolean isFailClosed() { return failClosed; }
        public void setFailClosed(boolean failClosed) { this.failClosed = failClosed; }
    }
}
