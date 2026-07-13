
package io.rosecloud.starter.security.config;

import io.rosecloud.common.security.credential.PasswordPolicy;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Maps {@code rosecloud.security.password-policy.*} into a {@link PasswordPolicy}
 * record. If no values are set, the defaults match ThingsBoard's recommended
 * baseline and preserve RoseCloud's original hardcoded rules.
 *
 * <p>Reference: ThingsBoard's {@code UserPasswordPolicy} supports min/max length,
 * character-type counts, {@code passwordExpirationPeriodDays}, and
 * {@code passwordReuseFrequencyDays}. RoseCloud's model adds
 * {@code allow-whitespace} for environments that need it.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "rosecloud.security.password-policy")
public class PasswordPolicyProperties {

    @Min(1)
    private int minLength = 8;

    @Min(1)
    private int maxLength = 64;

    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigit = true;
    private boolean requireSpecial = true;
    private boolean allowWhitespace = false;

    @Min(0)
    private int expirationDays = 0;

    @Min(0)
    private int reuseHistoryCount = 0;

    public PasswordPolicy toPolicy() {
        return new PasswordPolicy(minLength, maxLength, requireUppercase, requireLowercase,
                requireDigit, requireSpecial, allowWhitespace, expirationDays, reuseHistoryCount);
    }
}
