package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

/**
 * Repository port for persisted audit logs. The audit aspect publishes events;
 * a listener persists them through this port, and the query endpoint reads them.
 */
public interface AuditLogRepository {

    void insert(AuditLog log);

    java.util.Optional<AuditLog> findById(Long id);

    PageResult<AuditLog> page(long current, long size, Long tenantId, String action, String principal);
}
