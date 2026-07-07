package io.rosecloud.auth.error;

import io.rosecloud.common.core.error.ErrorCode;

/** Auth-service error codes (module prefix {@code auth}). */
public enum AuthErrorCode implements ErrorCode {

    BAD_CREDENTIALS("用户名或密码错误"),
    ACCOUNT_DISABLED("账号已停用"),
    INVALID_TOKEN("令牌无效或已过期"),
    ACCOUNT_LOCKED("账号已锁定，请稍后再试") {
        @Override
        public int httpStatus() {
            return 423;
        }
    },
    TOO_MANY_REQUESTS("请求过于频繁，请稍后再试") {
        @Override
        public int httpStatus() {
            return 429;
        }
    };

    private final String message;

    AuthErrorCode(String message) {
        this.message = message;
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
