package io.rosecloud.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for login brute-force protection. Bound to
 * {@code rosecloud.auth.login-protection.*}. All thresholds are configurable so
 * deployments can tighten or loosen the policy without code changes.
 */
@Component
@ConfigurationProperties(prefix = "rosecloud.auth.login-protection")
public class LoginProtectionProperties {

    /** Master switch for the whole protection layer. */
    private boolean enabled = true;

    /** Failed password attempts (per account) before the account is locked. */
    private int maxAttempts = 5;

    /** Sliding window for the per-account failure counter; resets on each failure. */
    private Duration lockWindow = Duration.ofMinutes(15);

    /** Lock duration for the first lock event; doubles on each subsequent event. */
    private Duration baseLockDuration = Duration.ofMinutes(15);

    /** Upper bound for an escalating lock duration. */
    private Duration maxLockDuration = Duration.ofHours(24);

    /** Failed attempts (per IP) before the IP is temporarily blocked. */
    private int ipMaxAttempts = 100;

    /** Window for the per-IP failure counter. */
    private Duration ipWindow = Duration.ofMinutes(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getLockWindow() {
        return lockWindow;
    }

    public void setLockWindow(Duration lockWindow) {
        this.lockWindow = lockWindow;
    }

    public Duration getBaseLockDuration() {
        return baseLockDuration;
    }

    public void setBaseLockDuration(Duration baseLockDuration) {
        this.baseLockDuration = baseLockDuration;
    }

    public Duration getMaxLockDuration() {
        return maxLockDuration;
    }

    public void setMaxLockDuration(Duration maxLockDuration) {
        this.maxLockDuration = maxLockDuration;
    }

    public int getIpMaxAttempts() {
        return ipMaxAttempts;
    }

    public void setIpMaxAttempts(int ipMaxAttempts) {
        this.ipMaxAttempts = ipMaxAttempts;
    }

    public Duration getIpWindow() {
        return ipWindow;
    }

    public void setIpWindow(Duration ipWindow) {
        this.ipWindow = ipWindow;
    }
}
