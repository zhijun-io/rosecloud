package io.rosecloud.starter.security.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for {@code rosecloud.security.oauth2.*} (JWT resource server). */
@ConfigurationProperties(prefix = "rosecloud.security.oauth2")
public class OAuth2Properties {

    /** JWK set URI for JWT signature validation. */
    private String jwkSetUri;

    /** Issuer URI for JWT validation (when the issuer claim must be verified). */
    private String issuerUri;

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }
}
