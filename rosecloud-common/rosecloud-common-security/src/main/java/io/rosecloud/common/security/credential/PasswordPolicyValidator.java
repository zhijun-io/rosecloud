package io.rosecloud.common.security.credential;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.exception.SecurityErrorCode;

/**
 * Central password policy checks, shared by auth and system services. Accepts a
 * {@link PasswordPolicy} at construction time so rules can be tuned via
 * configuration without code changes. ThingsBoard's {@code UserPasswordPolicy}
 * inspired the configurable expiry and reuse-history fields.
 *
 * <p>Usage: create a bean from {@code PasswordPolicyProperties} and inject into
 * the service that writes credentials.
 */
public class PasswordPolicyValidator {

    private final PasswordPolicy policy;

    public PasswordPolicyValidator(PasswordPolicy policy) {
        this.policy = policy;
    }

    /** Default policy matching the original hardcoded rules. */
    public static PasswordPolicyValidator withDefaults() {
        return new PasswordPolicyValidator(PasswordPolicy.defaults());
    }

    /** Current policy in effect, exposed for diagnostics / feature-info APIs. */
    public PasswordPolicy policy() {
        return policy;
    }

    public void validate(String password) {
        if (password == null) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        if (password.length() < policy.minLength() || password.length() > policy.maxLength()) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        if (!policy.allowWhitespace() && password.chars().anyMatch(Character::isWhitespace)) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        if (policy.requireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        if (policy.requireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        if (policy.requireDigit() && password.chars().noneMatch(Character::isDigit)) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        if (policy.requireSpecial()
                && password.chars().allMatch(ch -> Character.isLetterOrDigit((char) ch))) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
    }

    public void validateChange(String currentPassword, String newPassword) {
        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new BizException(SecurityErrorCode.PASSWORD_SAME_AS_OLD);
        }
        validate(newPassword);
    }

    /**
     * Check whether the given {@code passwordChangedAt} timestamp violates the
     * configured {@link PasswordPolicy#expirationDays()}. Returns true if the
     * password has expired and the user should be forced to reset it.
     * ThingsBoard's {@code UserPasswordPolicy.passwordExpirationPeriodDays}
     * has the same semantics.
     */
    public boolean isExpired(java.time.LocalDateTime passwordChangedAt) {
        if (policy.expirationDays() <= 0 || passwordChangedAt == null) {
            return false;
        }
        return passwordChangedAt.plusDays(policy.expirationDays())
                .isBefore(java.time.LocalDateTime.now());
    }
}
