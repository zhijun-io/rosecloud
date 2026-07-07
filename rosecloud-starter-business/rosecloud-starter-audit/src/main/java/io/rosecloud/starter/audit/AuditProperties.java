package io.rosecloud.starter.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Audit logging configuration under {@code rosecloud.audit.*}.
 */
@ConfigurationProperties(prefix = "rosecloud.audit")
public class AuditProperties {

    /** Master switch for the audit aspect; dormant when false. */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
