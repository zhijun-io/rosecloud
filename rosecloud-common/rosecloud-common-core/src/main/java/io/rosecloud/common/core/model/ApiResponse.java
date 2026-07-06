package io.rosecloud.common.core.model;

import io.rosecloud.common.core.error.ErrorCode;

public record ApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, "OK", "success", null);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.code(), errorCode.message(), null);
    }
}
