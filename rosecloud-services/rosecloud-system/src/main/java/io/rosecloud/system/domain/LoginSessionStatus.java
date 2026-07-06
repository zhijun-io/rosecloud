package io.rosecloud.system.domain;

/** Login-session status (stored as a tinyint code). */
public enum LoginSessionStatus {

    ONLINE(1),
    LOGGED_OUT(0);

    private final int code;

    LoginSessionStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static LoginSessionStatus of(int code) {
        for (LoginSessionStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown login session status: " + code);
    }
}
