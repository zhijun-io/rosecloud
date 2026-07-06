package io.rosecloud.starter.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for {@code rosecloud.lock.*}. */
@ConfigurationProperties(prefix = "rosecloud.lock")
public class LockProperties {

    /** Enable the distributed lock starter. */
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
