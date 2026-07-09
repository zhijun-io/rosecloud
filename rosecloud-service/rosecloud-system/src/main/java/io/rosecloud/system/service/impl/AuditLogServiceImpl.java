package io.rosecloud.system.service.impl;

import io.rosecloud.api.audit.AuditLogApi;
import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogRepository;
import io.rosecloud.system.service.AuditLogService;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.error.SystemErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
public class AuditLogServiceImpl implements AuditLogService, AuditLogApi {

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
    public PageResult<AuditLog> page(long current, long size, String action, String username) {
        return auditLogRepository.page(current, size, action, username);
    }

    @Override
    public void save(AuditLogRequest auditLogRequest) {
        AuditLog domain = new AuditLog(
                null,
                auditLogRequest.action(),
                auditLogRequest.description(),
                auditLogRequest.principal(),
                auditLogRequest.tenantId(),
                auditLogRequest.target(),
                auditLogRequest.elapsedMillis(),
                auditLogRequest.success(),
                auditLogRequest.error(),
                auditLogRequest.createTime()
        );
        auditLogRepository.insert(domain);
    }
}
