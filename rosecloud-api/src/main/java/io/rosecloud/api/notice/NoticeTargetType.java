package io.rosecloud.api.notice;

/** Notice audience scope (stored as a tinyint code). Shared by notice and system. */
public enum NoticeTargetType {

    GLOBAL(0),
    TENANT(1),
    ROLE(2),
    USER(3);

    private final int code;

    NoticeTargetType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static NoticeTargetType of(int code) {
        for (NoticeTargetType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown notice target type: " + code);
    }
}
