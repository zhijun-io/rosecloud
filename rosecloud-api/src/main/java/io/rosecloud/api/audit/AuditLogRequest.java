package io.rosecloud.api.audit;

import java.time.LocalDateTime;

public record AuditLogRequest(String action, String description, String principal, String tenantId, String target,
                               long elapsedMillis, boolean success, String error, LocalDateTime createTime,
                               String entityType, String entityId, String ipAddress, String severity) {

    public AuditLogRequest(String action, String description, String principal, String tenantId, String target,
                           long elapsedMillis, boolean success, String error, LocalDateTime createTime) {
        this(action, description, principal, tenantId, target, elapsedMillis, success, error, createTime,
                null, null, null, null);
    }
}
