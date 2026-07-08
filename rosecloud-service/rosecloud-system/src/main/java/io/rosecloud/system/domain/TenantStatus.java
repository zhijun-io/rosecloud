package io.rosecloud.system.domain;

/** Tenant lifecycle status (stored as a tinyint code). */
public enum TenantStatus {

    PENDING(0),
    ENABLED(1),
    DISABLED(2),
    EXPIRED(3);

    private final int code;

    TenantStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static TenantStatus of(int code) {
        for (TenantStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown tenant status: " + code);
    }
}
