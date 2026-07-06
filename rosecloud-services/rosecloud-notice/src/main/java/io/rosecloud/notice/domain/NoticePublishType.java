package io.rosecloud.notice.domain;

/** Notice publish mode (stored as a tinyint code). */
public enum NoticePublishType {

    IMMEDIATE(0),
    SCHEDULED(1);

    private final int code;

    NoticePublishType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
