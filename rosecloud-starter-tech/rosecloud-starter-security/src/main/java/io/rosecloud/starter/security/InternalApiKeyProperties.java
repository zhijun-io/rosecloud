package io.rosecloud.starter.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared secret for service-to-service calls to {@code /internal/**} endpoints.
 *
 * <p>The default value is a dev-only placeholder. Production deployments must
 * override it via {@code rosecloud.security.internal-api-key} or the
 * {@code ROSECLOUD_INTERNAL_API_KEY} environment variable; the value is treated
 * as a secret and should come from a vault / orchestrator secret, not source.
 */
@ConfigurationProperties(prefix = "rosecloud.security")
public class InternalApiKeyProperties {

    private String internalApiKey = "rosecloud-dev-internal-key";

    public String getInternalApiKey() {
        return internalApiKey;
    }

    public void setInternalApiKey(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }
}
