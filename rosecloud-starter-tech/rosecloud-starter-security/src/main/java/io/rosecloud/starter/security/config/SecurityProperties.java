package io.rosecloud.starter.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rosecloud.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private long accessTokenExpirationSeconds = 3600;
    private long refreshTokenExpirationSeconds = 86400;
    private String[] publicPaths = {"/api/auth/**", "/api/public/**", "/actuator/**", "/error"};

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }

    public long getAccessTokenExpirationSeconds() { return accessTokenExpirationSeconds; }
    public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) { this.accessTokenExpirationSeconds = accessTokenExpirationSeconds; }

    public long getRefreshTokenExpirationSeconds() { return refreshTokenExpirationSeconds; }
    public void setRefreshTokenExpirationSeconds(long refreshTokenExpirationSeconds) { this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds; }

    public String[] getPublicPaths() { return publicPaths; }
    public void setPublicPaths(String[] publicPaths) { this.publicPaths = publicPaths; }

    public static class Jwt {
        private String secret;
        private String issuer = "rosecloud";

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
    }
}
