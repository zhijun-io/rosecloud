package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.dto.TenantApplyRequest;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    public TenantServiceImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @AuditLog(action = "tenant-apply", description = "申请租户")
    @Override
    public Long apply(TenantApplyRequest request) {
        if (tenantRepository.existsByCode(request.code())) {
            throw new BizException(SystemErrorCode.TENANT_CODE_EXISTS);
        }
        Tenant tenant = new Tenant(null, request.name(), request.code(), TenantStatus.PENDING,
                request.contactUser(), request.contactPhone(), request.expireTime(), request.remark());
        return tenantRepository.insert(tenant);
    }

    @AuditLog(action = "tenant-open", description = "开通租户")
    @Override
    public void open(Long id) {
        Tenant tenant = load(id);
        if (tenant.status() != TenantStatus.PENDING) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        tenantRepository.updateStatus(id, TenantStatus.ENABLED);
    }

    @AuditLog(action = "tenant-disable", description = "停用租户")
    @Override
    public void disable(Long id) {
        Tenant tenant = load(id);
        if (tenant.status() != TenantStatus.ENABLED) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        tenantRepository.updateStatus(id, TenantStatus.DISABLED);
    }

    @AuditLog(action = "tenant-enable", description = "启用租户")
    @Override
    public void enable(Long id) {
        Tenant tenant = load(id);
        if (tenant.status() != TenantStatus.DISABLED) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        tenantRepository.updateStatus(id, TenantStatus.ENABLED);
    }

    @Override
    public PageResult<Tenant> page(long current, long size, String keyword) {
        return tenantRepository.page(current, size, keyword);
    }

    private Tenant load(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_NOT_FOUND));
    }
}
