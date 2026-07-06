package io.rosecloud.system.domain;

/** Task lifecycle status (stored as a tinyint code). */
public enum TaskStatus {

    PENDING(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3);

    private final int code;

    TaskStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static TaskStatus of(int code) {
        for (TaskStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown task status: " + code);
    }
}
