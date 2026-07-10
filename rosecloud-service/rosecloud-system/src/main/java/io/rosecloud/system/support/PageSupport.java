package io.rosecloud.system.support;

/**
 * Clamps raw pagination parameters so a caller cannot request an unbounded page size
 * (large query / DoS) or a non-positive page index.
 */
public final class PageSupport {

    public static final long MAX_SIZE = 100L;
    public static final long DEFAULT_SIZE = 10L;
    public static final long DEFAULT_CURRENT = 1L;

    private PageSupport() {
    }

    public static long current(long current) {
        return current < 1 ? DEFAULT_CURRENT : current;
    }

    public static long size(long size) {
        return size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
