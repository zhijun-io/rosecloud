package io.rosecloud.system.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a login-audit entry. ORM-free; mapped to/from {@code sys_login_log}. */
public final class LoginLog {

    private final Long id;
    private final String username;
    private final boolean success;
    private final String failReason;
    private final String ip;
    private final String userAgent;
    private final LocalDateTime loginTime;

    public LoginLog(Long id, String username, boolean success, String failReason, String ip, String userAgent,
                    LocalDateTime loginTime) {
        this.id = id;
        this.username = username;
        this.success = success;
        this.failReason = failReason;
        this.ip = ip;
        this.userAgent = userAgent;
        this.loginTime = loginTime;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public boolean isSuccess() { return success; }
    public String getFailReason() { return failReason; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public LocalDateTime getLoginTime() { return loginTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginLog that)) return false;
        return success == that.success && Objects.equals(id, that.id) && Objects.equals(username, that.username)
                && Objects.equals(failReason, that.failReason) && Objects.equals(ip, that.ip)
                && Objects.equals(userAgent, that.userAgent) && Objects.equals(loginTime, that.loginTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, success, failReason, ip, userAgent, loginTime);
    }

    @Override
    public String toString() {
        return "LoginLog[" +
                "id=" + id +
                ", username=" + username +
                ", success=" + success +
                ", failReason=" + failReason +
                ", ip=" + ip +
                ", userAgent=" + userAgent +
                ", loginTime=" + loginTime +
                ']';
    }
}
