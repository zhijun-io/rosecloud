package io.rosecloud.common.core.error;

/** Generic error codes shared across services (module prefix {@code CMM}). */
public enum CommonErrorCode implements ErrorCode {

    PARAM_INVALID("CMMA001", "参数校验失败"),
    INTERNAL_ERROR("CMME001", "系统内部错误");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
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
