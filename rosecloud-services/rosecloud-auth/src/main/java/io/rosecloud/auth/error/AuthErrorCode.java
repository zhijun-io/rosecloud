package io.rosecloud.auth.error;

import io.rosecloud.common.core.error.ErrorCode;

/** Auth-service error codes (module prefix {@code AUTH}). */
public enum AuthErrorCode implements ErrorCode {

    BAD_CREDENTIALS("AUTHA001", "用户名或密码错误"),
    ACCOUNT_DISABLED("AUTHA002", "账号已停用"),
    INVALID_TOKEN("AUTHA003", "令牌无效或已过期");

    private final String code;
    private final String message;

    AuthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
