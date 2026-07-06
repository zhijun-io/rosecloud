package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import java.time.LocalDate;

/** MyBatis-Plus persistent object for {@code sys_tenant}; confined to infrastructure. */
@TableName("sys_tenant")
public class TenantPO extends BaseEntity {

    private String name;
    private String code;
    private Integer status;
    private String contactUser;
    private String contactPhone;
    private LocalDate expireTime;
    private String remark;
    private String adminUsername;
    private String adminPassword;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getContactUser() { return contactUser; }
    public void setContactUser(String contactUser) { this.contactUser = contactUser; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public LocalDate getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDate expireTime) { this.expireTime = expireTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
}
