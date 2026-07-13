package io.rosecloud.common.security.credential;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.exception.SecurityErrorCode;

/**
 * Central password complexity checks, shared by auth (the authoritative writer) and system.
 * Lives in the common security library so both services validate against one rule set with
 * zero microservice dependencies.
 */
public final class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    private PasswordPolicyValidator() {
    }

    public static void validate(String password) {
        if (password == null) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!upper || !lower || !digit || !special) {
            throw new BizException(SecurityErrorCode.PASSWORD_TOO_WEAK);
        }
    }

    public static void validateChange(String currentPassword, String newPassword) {
        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new BizException(SecurityErrorCode.PASSWORD_SAME_AS_OLD);
        }
        validate(newPassword);
    }
}
