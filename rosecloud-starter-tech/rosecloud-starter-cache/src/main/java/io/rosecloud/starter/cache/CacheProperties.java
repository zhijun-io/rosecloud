package io.rosecloud.starter.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for {@code rosecloud.cache.*}. */
@ConfigurationProperties(prefix = "rosecloud.cache")
public class CacheProperties {

    /** Enable the cache starter. */
    private boolean enabled = false;

    /** Backend type: {@code in-memory} (default, single-instance) or {@code redis} (cross-instance). */
    private String type = "in-memory";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
