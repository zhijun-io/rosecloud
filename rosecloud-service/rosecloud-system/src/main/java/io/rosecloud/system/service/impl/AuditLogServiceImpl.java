package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogRepository;
import io.rosecloud.system.service.AuditLogService;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.error.SystemErrorCode;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog get(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.AUDIT_LOG_NOT_FOUND));
    }

    @Override
    public PageResult<AuditLog> page(long current, long size, String tenantId, String action, String principal) {
        return auditLogRepository.page(current, size, tenantId, action, principal);
    }
}
