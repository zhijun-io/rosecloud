package io.rosecloud.common.core.error;

/** Generic error codes shared across services (module prefix {@code common}). */
public enum CommonErrorCode implements ErrorCode {

    PARAM_INVALID("参数校验失败"),
    INTERNAL_ERROR("系统内部错误");

    private final String message;

    CommonErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public String modulePrefix() {
        return "common";
    }

}
