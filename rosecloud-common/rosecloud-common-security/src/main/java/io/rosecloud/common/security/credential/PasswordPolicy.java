
package io.rosecloud.common.security.credential;

/**
 * Configurable password complexity rules, modelled after ThingsBoard's
 * {@code UserPasswordPolicy}. Services produce one from properties and pass
 * it to {@link PasswordPolicyValidator}; the default matches the original
 * hardcoded policy so existing behaviour is preserved without configuration.
 *
 * @param minLength        minimum password length
 * @param maxLength        maximum password length
 * @param requireUppercase at least one upper-case letter
 * @param requireLowercase at least one lower-case letter
 * @param requireDigit     at least one digit
 * @param requireSpecial   at least one special character
 * @param allowWhitespace  whether whitespace is permitted inside the password
 * @param expirationDays   password must be changed after this many days (0 = never expires)
 * @param reuseHistoryCount number of previous passwords to check (0 = no check)
 */
public record PasswordPolicy(
        int minLength,
        int maxLength,
        boolean requireUppercase,
        boolean requireLowercase,
        boolean requireDigit,
        boolean requireSpecial,
        boolean allowWhitespace,
        int expirationDays,
        int reuseHistoryCount) {

    public PasswordPolicy {
        if (minLength < 1) {
            throw new IllegalArgumentException("minLength must be >= 1");
        }
        if (maxLength < minLength) {
            throw new IllegalArgumentException("maxLength must be >= minLength");
        }
        if (expirationDays < 0) {
            throw new IllegalArgumentException("expirationDays must not be negative");
        }
        if (reuseHistoryCount < 0) {
            throw new IllegalArgumentException("reuseHistoryCount must not be negative");
        }
    }

    public static PasswordPolicy defaults() {
        return new PasswordPolicy(8, 64, true, true, true, true, false, 0, 0);
    }

    public static PasswordPolicy permissive() {
        return new PasswordPolicy(6, 128, false, false, false, false, true, 0, 0);
    }
}
