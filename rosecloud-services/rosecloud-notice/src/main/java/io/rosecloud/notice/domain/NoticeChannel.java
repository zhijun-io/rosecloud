package io.rosecloud.notice.domain;

/** Notice delivery channels, stored as a bitmask on the notice. */
public enum NoticeChannel {

    STATION(1),
    EMAIL(2),
    SMS(4);

    private final int code;

    NoticeChannel(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean in(Integer mask) {
        return mask != null && (mask & code) != 0;
    }

    /** Default mask when none is specified: station only (pull feed). */
    public static int defaultMask() {
        return STATION.code;
    }

    public static int maskOf(Integer mask) {
        return mask == null ? defaultMask() : mask;
    }
}
