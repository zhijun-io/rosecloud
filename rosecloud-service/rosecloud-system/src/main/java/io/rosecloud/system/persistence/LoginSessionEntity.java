package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

/** MyBatis-Plus persistent object for {@code sys_login_session}; confined to infrastructure. */
@TableName("sys_login_session")
public class LoginSessionEntity extends BaseEntity {

    private String jti;
    private Long userId;
    private String username;
    private String tenantId;
    private LocalDateTime loginTime;
    private LocalDateTime expireTime;
    private String ip;
    private String userAgent;
    private Integer status;

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
