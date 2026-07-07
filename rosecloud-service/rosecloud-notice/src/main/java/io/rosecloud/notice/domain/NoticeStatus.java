package io.rosecloud.notice.domain;

/** Notice lifecycle status (stored as a tinyint code). */
public enum NoticeStatus {

    DRAFT(0),
    PUBLISHED(1);

    private final int code;

    NoticeStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
