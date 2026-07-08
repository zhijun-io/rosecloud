package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;

public interface AuditLogService {

    AuditLog get(Long id);

    PageResult<AuditLog> page(long current, long size, Long tenantId, String action, String principal);
}
