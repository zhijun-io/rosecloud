package io.rosecloud.system.domain;

/**
 * Storage-agnostic filter for audit-log queries (everything except paging and the time
 * window, which live in {@link io.rosecloud.common.core.model.TimePageQuery}). {@code null}
 * fields are unset (treated as "no filter"). Tenant scoping is resolved by the service: an
 * explicit {@link #tenantId()} overrides the caller's tenant context, enabling platform-admin
 * cross-tenant queries.
 */
public record AuditLogQuery(String action, String principal, String tenantId, Boolean success,
                            String entityType) {

    public static AuditLogQuery of(String action, String principal, String tenantId, Boolean success,
                                   String entityType) {
        return new AuditLogQuery(action, principal, tenantId, success, entityType);
    }
}
