package io.rosecloud.starter.sequence;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for {@code rosecloud.sequence.*}. */
@ConfigurationProperties(prefix = "rosecloud.sequence")
public class SequenceProperties {

    /** Enable the sequence generator starter. */
    private boolean enabled = false;

    /** Backend type: {@code in-memory} (default, single-instance) or {@code redis} (cross-instance, persistent). */
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
