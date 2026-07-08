package io.rosecloud.system.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Domain view of a login session. ORM-free; mapped to/from {@code sys_login_session}. */
public final class LoginSession {

    private final Long id;
    private final String jti;
    private final Long userId;
    private final String username;
    private final Long tenantId;
    private final LocalDateTime loginTime;
    private final LocalDateTime expireTime;
    private final String ip;
    private final String userAgent;
    private final Integer status;

    public LoginSession(Long id, String jti, Long userId, String username, Long tenantId, LocalDateTime loginTime,
                        LocalDateTime expireTime, String ip, String userAgent, Integer status) {
        this.id = id;
        this.jti = jti;
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.loginTime = loginTime;
        this.expireTime = expireTime;
        this.ip = ip;
        this.userAgent = userAgent;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getJti() { return jti; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public Long getTenantId() { return tenantId; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public Integer getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginSession that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(jti, that.jti) && Objects.equals(userId, that.userId)
                && Objects.equals(username, that.username) && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(loginTime, that.loginTime) && Objects.equals(expireTime, that.expireTime)
                && Objects.equals(ip, that.ip) && Objects.equals(userAgent, that.userAgent)
                && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jti, userId, username, tenantId, loginTime, expireTime, ip, userAgent, status);
    }

    @Override
    public String toString() {
        return "LoginSession[" +
                "id=" + id +
                ", jti=" + jti +
                ", userId=" + userId +
                ", username=" + username +
                ", tenantId=" + tenantId +
                ", loginTime=" + loginTime +
                ", expireTime=" + expireTime +
                ", ip=" + ip +
                ", userAgent=" + userAgent +
                ", status=" + status +
                ']';
    }
}
