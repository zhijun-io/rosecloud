package io.rosecloud.notice.error;

import io.rosecloud.common.core.error.ErrorCode;

/** Notice-service error codes (module prefix {@code notice}). */
public enum NoticeErrorCode implements ErrorCode {

    NOTICE_NOT_FOUND("通知不存在"),
    NOTICE_NOT_VISIBLE("无权查看该通知"),
    NOTICE_NOT_CONFIRMABLE("该通知无需确认");

    private final String message;

    NoticeErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
