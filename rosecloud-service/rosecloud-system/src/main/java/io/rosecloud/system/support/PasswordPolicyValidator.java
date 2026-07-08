package io.rosecloud.system.support;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.error.SystemErrorCode;

/** Central password complexity checks shared by activation and self-service change password flows. */
public final class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    private PasswordPolicyValidator() {
    }

    public static void validate(String password) {
        if (password == null) {
            throw new BizException(SystemErrorCode.PASSWORD_TOO_WEAK);
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BizException(SystemErrorCode.PASSWORD_TOO_WEAK);
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            throw new BizException(SystemErrorCode.PASSWORD_TOO_WEAK);
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!upper || !lower || !digit || !special) {
            throw new BizException(SystemErrorCode.PASSWORD_TOO_WEAK);
        }
    }

    public static void validateChange(String currentPassword, String newPassword) {
        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new BizException(SystemErrorCode.PASSWORD_SAME_AS_OLD);
        }
        validate(newPassword);
    }
}
