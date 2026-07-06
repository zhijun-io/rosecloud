package io.rosecloud.system.error;

import io.rosecloud.common.core.error.ErrorCode;

/** System-service error codes (module prefix {@code SYS}). */
public enum SystemErrorCode implements ErrorCode {

    TENANT_CODE_EXISTS("SYSA001", "租户编码已存在"),
    TENANT_NOT_FOUND("SYSA002", "租户不存在"),
    TENANT_STATUS_INVALID("SYSA003", "租户当前状态不允许该操作");

    private final String code;
    private final String message;

    SystemErrorCode(String code, String message) {
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
