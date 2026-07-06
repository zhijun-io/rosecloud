package io.rosecloud.starter.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for {@code rosecloud.security.token-revocation.*}. */
@ConfigurationProperties(prefix = "rosecloud.security.token-revocation")
public class TokenRevocationProperties {

    /** Revocation store type: {@code in-memory} (default, single-instance) or {@code redis} (shared). */
    private String type = "in-memory";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
