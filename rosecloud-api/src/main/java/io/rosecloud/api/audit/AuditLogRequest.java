package io.rosecloud.api.audit;

import java.time.LocalDateTime;

public record AuditLogRequest(String action, String description, String principal, String tenantId, String target,
                              long elapsedMillis, boolean success, String error, LocalDateTime createTime) {
}
