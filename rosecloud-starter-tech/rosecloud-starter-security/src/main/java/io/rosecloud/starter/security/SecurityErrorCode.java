package io.rosecloud.starter.security;

import io.rosecloud.common.core.error.ErrorCode;

/**
 * Security-starter error codes (module prefix {@code security}).
 */
public enum SecurityErrorCode implements ErrorCode {

    INVALID_TOKEN("令牌无效或已过期"),
    INTERNAL_API_KEY_INVALID("内部访问需要有效的 API 密钥"),
    BAD_CREDENTIALS("用户名或密码错误"),
    ACCOUNT_DISABLED("账号已停用"),
    UNAUTHORIZED("未登录或登录已过期") {
        @Override
        public int httpStatus() {
            return 401;
        }
    },
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

    SecurityErrorCode(String message) {
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
