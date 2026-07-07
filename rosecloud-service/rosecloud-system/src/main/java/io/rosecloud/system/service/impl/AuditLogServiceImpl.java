package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogRepository;
import io.rosecloud.system.service.AuditLogService;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public PageResult<AuditLog> page(long current, long size, String action, String principal) {
        return auditLogRepository.page(current, size, action, principal);
    }
}
