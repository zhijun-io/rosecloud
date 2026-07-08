package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

/** MyBatis-Plus persistent entity for {@code sys_user}; confined to infrastructure. */
@TableName("sys_user")
public class UserEntity extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private Integer status;
    private Long tenantId;
    private String email;
    private String phone;
    private String extra;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}
