package io.rosecloud.system.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe binding for user activation settings, replacing the three ad-hoc
 * {@code @Value} placeholders previously inlined in {@code UserActivationServiceImpl}.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "rosecloud.user")
public class UserActivationProperties {

    @Min(1)
    private long activationTtlHours = 24;

    private String activationLinkBaseUrl = "";

    @Min(10)
    private long resendCooldownSeconds = 60;
}
