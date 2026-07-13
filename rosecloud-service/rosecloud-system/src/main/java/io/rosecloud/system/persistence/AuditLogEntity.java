package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.common.core.model.ToData;
import io.rosecloud.starter.data.BaseEntity;
import io.rosecloud.system.domain.AuditLog;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_audit_log}; confined to infrastructure. */
@TableName("sys_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntity extends BaseEntity implements ToData<AuditLog> {

    private String action;
    private String description;
    private String principal;
    private String tenantId;
    private String target;
    private Long elapsedMillis;
    private Integer success;
    private String error;
    private String entityType;
    private String entityId;
    private String ipAddress;
    private String severity;

    @Override
    public AuditLog toData() {
        return new AuditLog(getId(), action, description, principal, tenantId, target,
                elapsedMillis == null ? 0L : elapsedMillis, success != null && success == 1,
                error, getCreateTime(), entityType, entityId, ipAddress, severity);
    }
}
