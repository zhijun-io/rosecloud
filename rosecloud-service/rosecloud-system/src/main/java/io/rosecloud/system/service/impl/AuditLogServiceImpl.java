package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import io.rosecloud.api.audit.AuditLogApi;
import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.TimePageQuery;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogQuery;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.AuditLogRepository;
import io.rosecloud.system.service.AuditLogService;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import org.springframework.stereotype.Service;

/** Audit-log application service. Delegates persistence to {@link AuditLogRepository}. */
@RequiredArgsConstructor
@Service
public class AuditLogServiceImpl implements AuditLogService, AuditLogApi {

    private final AuditLogRepository auditLogRepository;
    @Override
    public AuditLog get(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id);
        if (auditLog == null) {
            throw new BizException(SystemErrorCode.AUDIT_LOG_NOT_FOUND);
        }
        return auditLog;
    }

    @Override
    public PagedData<AuditLog> page(TimePageQuery pageQuery, AuditLogQuery query) {
        AuditLogQuery resolved = resolveTenant(query);
        return auditLogRepository.page(pageQuery, resolved);
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
                query.entityType());
    }
}
