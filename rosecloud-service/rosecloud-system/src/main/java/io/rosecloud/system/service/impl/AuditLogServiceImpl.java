package io.rosecloud.system.service.impl;

import io.rosecloud.api.audit.AuditLogApi;
import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogQuery;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.AuditLogRepository;
import io.rosecloud.system.service.AuditLogService;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import org.springframework.stereotype.Service;

/** Audit-log application service. Delegates persistence to {@link AuditLogRepository}. */
@Service
public class AuditLogServiceImpl implements AuditLogService, AuditLogApi {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog get(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id);
        if (auditLog == null) {
            throw new BizException(SystemErrorCode.AUDIT_LOG_NOT_FOUND);
        }
        return auditLog;
    }

    @Override
    public PageResult<AuditLog> page(long current, long size, AuditLogQuery query) {
        AuditLogQuery resolved = resolveTenant(query);
        return auditLogRepository.page(current, size, resolved);
    }

    @Override
    public void save(AuditLogRequest auditLogRequest) {
        auditLogRepository.save(auditLogRequest);
    }

    /**
     * Tenant scoping: when no explicit tenant is requested, scope to the caller's
     * tenant context. A platform admin may pass a tenant id to query cross-tenant.
     */
    private AuditLogQuery resolveTenant(AuditLogQuery query) {
        if (query.tenantId() != null) {
            return query;
        }
        String contextTenant = TenantContextHolder.getTenantId();
        if (contextTenant == null) {
            return query;
        }
        return AuditLogQuery.of(query.action(), query.principal(), contextTenant, query.success(),
                query.entityType(), query.startTime(), query.endTime());
    }
}
