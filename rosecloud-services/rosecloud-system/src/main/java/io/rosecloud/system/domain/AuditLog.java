package io.rosecloud.system.domain;

import java.time.LocalDateTime;

/** Domain view of a persisted audit entry. ORM-free; mapped to/from {@code sys_audit_log}. */
public record AuditLog(Long id, String action, String description, String principal, Long tenantId,
                       String target, long elapsedMillis, boolean success, String error,
                       LocalDateTime createTime) {
}
