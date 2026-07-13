package io.rosecloud.system.persistence;

import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.TimePageQuery;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogQuery;

/**
 * Storage abstraction for audit logs. The default MyBatis-Plus-backed implementation
 * is {@link AuditLogRepositoryImpl}; swap the storage backend (e.g. Elasticsearch)
 * by providing a different {@code AuditLogRepository} bean.
 */
public interface AuditLogRepository {

    void save(AuditLogRequest request);

    AuditLog findById(Long id);

    PagedData<AuditLog> page(TimePageQuery pageQuery, AuditLogQuery query);
}
