package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

/** MyBatis-Plus persistent entity for {@code sys_audit_log}; confined to infrastructure. */
@TableName("sys_audit_log")
public class AuditLogEntity extends BaseEntity {

    private String action;
    private String description;
    private String principal;
    private Long tenantId;
    private String target;
    private Long elapsedMillis;
    private Integer success;
    private String error;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPrincipal() { return principal; }
    public void setPrincipal(String principal) { this.principal = principal; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public Long getElapsedMillis() { return elapsedMillis; }
    public void setElapsedMillis(Long elapsedMillis) { this.elapsedMillis = elapsedMillis; }
    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
