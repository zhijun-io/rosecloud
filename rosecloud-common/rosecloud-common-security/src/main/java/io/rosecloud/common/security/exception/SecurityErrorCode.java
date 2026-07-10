package io.rosecloud.common.security.exception;

import io.rosecloud.common.core.error.ErrorCode;

public enum SecurityErrorCode implements ErrorCode {

    UNAUTHORIZED("未授权，需要登录"),
    USER_NOT_FOUND("用户不存在"),
    USER_DISABLED("用户已禁用"),
    BAD_CREDENTIALS("用户名或密码错误"),
    FORBIDDEN("无权限访问"),
    TENANT_UNAVAILABLE("租户不可用"),
    TENANT_DISABLED("租户已禁用"),
    TENANT_PENDING("租户待激活");

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
        return switch (this) {
            case UNAUTHORIZED, BAD_CREDENTIALS,
                 TENANT_UNAVAILABLE, TENANT_DISABLED, TENANT_PENDING -> 401;
            case FORBIDDEN -> 403;
            default -> 400;
        };
    }
}
