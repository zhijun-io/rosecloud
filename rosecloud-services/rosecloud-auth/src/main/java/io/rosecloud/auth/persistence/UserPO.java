package io.rosecloud.auth.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

/**
 * MyBatis-Plus persistent object for {@code sys_user}. Confined to the
 * infrastructure layer; the domain layer never sees it.
 */
@TableName("sys_user")
public class UserPO extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private Integer status;
    private Long tenantId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
