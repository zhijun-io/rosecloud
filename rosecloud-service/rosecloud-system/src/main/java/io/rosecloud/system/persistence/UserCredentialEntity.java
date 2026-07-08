package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDateTime;

/** MyBatis-Plus persistent entity for {@code user_credential}; confined to infrastructure. */
@TableName("user_credential")
public class UserCredentialEntity extends BaseEntity {

    private Long userId;
    private String password;
    private LocalDateTime passwordChangedTime;
    private String activateToken;
    private LocalDateTime expireTime;
    private LocalDateTime usedTime;
    private LocalDateTime sendTime;
    private LocalDateTime lastLoginTime;
    private Long version;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDateTime getPasswordChangedTime() { return passwordChangedTime; }
    public void setPasswordChangedTime(LocalDateTime passwordChangedTime) { this.passwordChangedTime = passwordChangedTime; }
    public String getActivateToken() { return activateToken; }
    public void setActivateToken(String activateToken) { this.activateToken = activateToken; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getUsedTime() { return usedTime; }
    public void setUsedTime(LocalDateTime usedTime) { this.usedTime = usedTime; }
    public LocalDateTime getSendTime() { return sendTime; }
    public void setSendTime(LocalDateTime sendTime) { this.sendTime = sendTime; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
