package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;

public interface AuditLogService {

    PageResult<AuditLog> page(long current, long size, String action, String principal);
}
