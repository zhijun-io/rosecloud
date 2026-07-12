package io.rosecloud.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding for user activation settings, replacing the three ad-hoc
 * {@code @Value} placeholders previously inlined in {@code UserActivationServiceImpl}.
 */
@ConfigurationProperties(prefix = "rosecloud.user")
public class UserActivationProperties {

    private long activationTtlHours = 24;
    private String activationLinkBaseUrl = "";
    private long resendCooldownSeconds = 60;

    public long getActivationTtlHours() {
        return activationTtlHours;
    }

    public void setActivationTtlHours(long activationTtlHours) {
        this.activationTtlHours = activationTtlHours;
    }

    public String getActivationLinkBaseUrl() {
        return activationLinkBaseUrl;
    }

    public void setActivationLinkBaseUrl(String activationLinkBaseUrl) {
        this.activationLinkBaseUrl = activationLinkBaseUrl;
    }

    public long getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(long resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }
}
