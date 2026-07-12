package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasTenantId;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Value;

/** Domain view of a persisted audit entry. ORM-free; mapped to/from {@code sys_audit_log}. */
@Value
@AllArgsConstructor
public final class AuditLog implements HasId, HasTenantId {

    Long id;
    String action;
    String description;
    String principal;
    String tenantId;
    String target;
    long elapsedMillis;
    boolean success;
    String error;
    LocalDateTime createTime;
}
