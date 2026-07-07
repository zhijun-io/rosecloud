package io.rosecloud.starter.security;

import io.rosecloud.common.core.error.ErrorCode;

/** Security-starter error codes (module prefix {@code security}). */
public enum SecurityErrorCode implements ErrorCode {

    INVALID_TOKEN("令牌无效或已过期"),
    INTERNAL_API_KEY_INVALID("内部访问需要有效的 API 密钥");

    private final String message;

    SecurityErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String modulePrefix() {
        return "security";
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
