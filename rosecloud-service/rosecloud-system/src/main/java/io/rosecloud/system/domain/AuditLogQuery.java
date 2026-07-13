package io.rosecloud.system.domain;

import java.time.LocalDateTime;

/**
 * Storage-agnostic filter for audit-log queries. {@code null} fields are unset
 * (treated as "no filter"). Tenant scoping is resolved by the service: an explicit
 * {@link #tenantId()} overrides the caller's tenant context, enabling platform-admin
 * cross-tenant queries.
 */
public record AuditLogQuery(String action, String principal, String tenantId, Boolean success,
                            String entityType, LocalDateTime startTime, LocalDateTime endTime) {

    public static AuditLogQuery of(String action, String principal, String tenantId, Boolean success,
                                   String entityType, LocalDateTime startTime, LocalDateTime endTime) {
        return new AuditLogQuery(action, principal, tenantId, success, entityType, startTime, endTime);
    }
}
