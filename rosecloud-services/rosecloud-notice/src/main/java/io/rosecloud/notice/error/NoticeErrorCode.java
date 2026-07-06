package io.rosecloud.notice.error;

import io.rosecloud.common.core.error.ErrorCode;

/** Notice-service error codes (module prefix {@code NTC}). */
public enum NoticeErrorCode implements ErrorCode {

    NOTICE_NOT_FOUND("NTCA001", "通知不存在"),
    NOTICE_NOT_VISIBLE("NTCA002", "无权查看该通知"),
    NOTICE_NOT_CONFIRMABLE("NTCA003", "该通知无需确认");

    private final String code;
    private final String message;

    NoticeErrorCode(String code, String message) {
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
