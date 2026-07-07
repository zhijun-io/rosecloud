package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

/** MyBatis-Plus persistent object for {@code sys_login_log}; confined to infrastructure. */
@TableName("sys_login_log")
public class LoginLogPO extends BaseEntity {

    private String username;
    private Integer success;
    private String failReason;
    private String ip;
    private String userAgent;
    private LocalDateTime loginTime;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
}
