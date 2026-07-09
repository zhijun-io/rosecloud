package io.rosecloud.starter.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "rosecloud.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private long accessTokenExpirationSeconds = 3600;
    private long refreshTokenExpirationSeconds = 86400;
    private String[] publicPaths = {"/api/auth/**", "/api/noauth/**", "/api/public/**", "/actuator/health/**", "/error"};
    private Cors cors = new Cors();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }

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

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
    }
}
