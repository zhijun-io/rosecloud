package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import io.rosecloud.starter.data.BaseEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** MyBatis-Plus persistent entity for {@code sys_audit_log}; confined to infrastructure. */
@TableName("sys_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntity extends BaseEntity {

    private String action;
    private String description;
    private String principal;
    private String tenantId;
    private String target;
    private Long elapsedMillis;
    private Integer success;
    private String error;
}
