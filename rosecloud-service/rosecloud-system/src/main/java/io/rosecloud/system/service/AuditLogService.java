package io.rosecloud.system.service;

import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogQuery;

import java.util.Map;

public interface AuditLogService {

    AuditLog get(Long id);

    PageResult<AuditLog> page(long current, long size, AuditLogQuery query);

    void save(AuditLogRequest auditLogRequest);
}
