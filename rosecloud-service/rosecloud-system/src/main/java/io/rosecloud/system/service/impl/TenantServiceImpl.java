package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantProfileRepository;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.TenantProvisioner;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.dto.TenantCreateRequest;
import io.rosecloud.system.service.dto.TenantUpdateRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final TenantProfileRepository tenantProfileRepository;
    private final TenantProvisioner tenantProvisioner;

    public TenantServiceImpl(TenantRepository tenantRepository, TenantProfileRepository tenantProfileRepository,
                             TenantProvisioner tenantProvisioner) {
        this.tenantRepository = tenantRepository;
        this.tenantProfileRepository = tenantProfileRepository;
        this.tenantProvisioner = tenantProvisioner;
    }

    @AuditLog(action = "tenant-create", description = "创建租户")
    @Override
    public String create(TenantCreateRequest request) {
        String id = persistTenant(request);
        open(id);
        return id;
    }

    @AuditLog(action = "tenant-update", description = "修改租户")
    @Override
    public void update(String id, TenantUpdateRequest request) {
        Tenant tenant = load(id);
        String tenantProfileId = resolveTenantProfileId(
                request.tenantProfileId() == null || request.tenantProfileId().isBlank()
                        ? tenant.getTenantProfileId() : request.tenantProfileId());
        Tenant updated = new Tenant(id, request.name(), tenant.getStatus(), request.contactUser(),
                request.contactPhone(), request.expireTime(), request.remark(), tenantProfileId,
                tenant.getAdditionalInfo());
        tenantRepository.update(updated);
    }

    @AuditLog(action = "tenant-delete", description = "删除租户")
    @Override
    public void delete(String id) {
        load(id);
        tenantRepository.deleteById(id);
    }

    @Override
    public Tenant get(String id) {
        return load(id);
    }

    /**
     * Dispatches tenant provisioning asynchronously. Returns the tenant id so
     * the caller can track the tenant record; the tenant stays
     * {@link TenantStatus#PENDING} until provisioning succeeds.
     */
    @AuditLog(action = "tenant-open", description = "开通租户")
    @Override
    public String open(String id) {
        Tenant tenant = load(id);
        tenant.getStatus().transitionTo(TenantStatus.ENABLED);
        tenantProvisioner.provision(id);
        return id;
    }

    @AuditLog(action = "tenant-disable", description = "停用租户")
    @Override
    public void disable(String id) {
        Tenant tenant = load(id);
        tenantRepository.updateStatus(id, tenant.getStatus().transitionTo(TenantStatus.DISABLED));
    }

    @AuditLog(action = "tenant-enable", description = "启用租户")
    @Override
    public void enable(String id) {
        Tenant tenant = load(id);
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDate.now())) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        tenantRepository.updateStatus(id, tenant.getStatus().transitionTo(TenantStatus.ENABLED));
    }

    @Override
    public PageResult<Tenant> page(long current, long size, String keyword) {
        return tenantRepository.page(current, size, keyword);
    }

    private Tenant load(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_NOT_FOUND));
    }

    private String persistTenant(TenantCreateRequest request) {
        String tenantId = UUID.randomUUID().toString();
        String tenantProfileId = resolveTenantProfileId(request.tenantProfileId());
        Tenant tenant = new Tenant(tenantId, request.name(), TenantStatus.PENDING,
                request.contactUser(), request.contactPhone(), request.expireTime(), request.remark(),
                tenantProfileId, null);
        return tenantRepository.insert(tenant, request.adminUsername());
    }

    private String resolveTenantProfileId(String tenantProfileId) {
        if (tenantProfileId == null || tenantProfileId.isBlank()) {
            return tenantProfileRepository.defaultProfileId();
        }
        tenantProfileRepository.findById(tenantProfileId)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
        return tenantProfileId;
    }

}
