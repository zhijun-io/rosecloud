package io.rosecloud.auth.error;

import io.rosecloud.common.core.error.ErrorCode;

/** Auth-service error codes (module prefix {@code AUTH}). */
public enum AuthErrorCode implements ErrorCode {

    BAD_CREDENTIALS("AUTHA001", "用户名或密码错误"),
    ACCOUNT_DISABLED("AUTHA002", "账号已停用"),
    INVALID_TOKEN("AUTHA003", "令牌无效或已过期"),
    ACCOUNT_LOCKED("AUTHA004", "账号已锁定，请稍后再试") {
        @Override
        public int httpStatus() {
            return 423;
        }
    },
    TOO_MANY_REQUESTS("AUTHA005", "请求过于频繁，请稍后再试") {
        @Override
        public int httpStatus() {
            return 429;
        }
    };

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

    @Override
    public int httpStatus() {
        return 401;
    }
}
