package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantDao;
import io.rosecloud.system.persistence.TenantEntity;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.persistence.TenantProfileDao;
import io.rosecloud.system.service.TenantProvisioner;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.dto.TenantCreateRequest;
import io.rosecloud.system.service.dto.TenantUpdateRequest;
import io.rosecloud.system.support.TenantIdSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class TenantServiceImpl implements TenantService {

    private final TenantDao tenantDao;
    private final TenantProfileDao tenantProfileDao;
    private final TenantProvisioner tenantProvisioner;
    private final EntityCache<String, Tenant> tenantCache;
    private final EntityEventPublisher eventPublisher;

    @AuditLog(action = "tenant-create", description = "创建租户")
    @Override
    public String create(TenantCreateRequest request) {
        String id = persistTenant(request);
        open(id);
        eventPublisher.publish(EntityChangedEvent.created(
                EntityCacheNames.TENANT, id, id, null));
        return id;
    }

    @AuditLog(action = "tenant-update", description = "修改租户")
    @Override
    public void update(String id, TenantUpdateRequest request) {
        String tenantId = requireTenantId(id);
        Tenant tenant = load(tenantId);
        String tenantProfileId = resolveTenantProfileId(
                request.tenantProfileId() == null || request.tenantProfileId().isBlank()
                        ? tenant.getTenantProfileId() : request.tenantProfileId());
        Tenant updated = new Tenant(tenantId, request.name(), tenant.getStatus(), request.contactUser(),
                request.contactPhone(), request.expireTime(), request.remark(), tenantProfileId,
                tenant.getAdditionalInfo());
        tenantDao.save(updated);
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.TENANT, tenantId, tenantId, tenant, updated));
    }

    @AuditLog(action = "tenant-delete", description = "删除租户")
    @Transactional
    @Override
    public void delete(String id) {
        String tenantId = requireTenantId(id);
        Tenant tenant = load(tenantId);
        // Cascade-delete all tenant-scoped resources before removing the tenant record.
        // 借鉴 ThingsBoard TenantServiceImpl.deleteTenant() 的级联清理顺序。
        tenantProvisioner.deprovision(tenantId);
        tenantDao.removeById(tenantId);
        eventPublisher.publish(EntityChangedEvent.deleted(
                EntityCacheNames.TENANT, tenantId, tenantId, tenant));
    }

    @Override
    public Tenant get(String id) {
        return load(requireTenantId(id));
    }

    /**
     * Dispatches tenant provisioning asynchronously. Returns the tenant id so
     * the caller can track the tenant record; the tenant stays
     * {@link TenantStatus#PENDING} until provisioning succeeds.
     */
    @AuditLog(action = "tenant-open", description = "开通租户")
    @Override
    public String open(String id) {
        String tenantId = requireTenantId(id);
        Tenant tenant = load(tenantId);
        tenant.getStatus().transitionTo(TenantStatus.ENABLED);
        tenantProvisioner.provision(tenantId);
        return tenantId;
    }

    @AuditLog(action = "tenant-disable", description = "停用租户")
    @Override
    public void disable(String id) {
        String tenantId = requireTenantId(id);
        Tenant tenant = load(tenantId);
        int newStatusCode = tenant.getStatus().transitionTo(TenantStatus.DISABLED).code();
        tenantDao.updateStatus(tenantId, newStatusCode);
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.TENANT, tenantId, tenantId, tenant, null));
    }

    @AuditLog(action = "tenant-enable", description = "启用租户")
    @Override
    public void enable(String id) {
        String tenantId = requireTenantId(id);
        Tenant tenant = load(tenantId);
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDate.now())) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        int newStatusCode = tenant.getStatus().transitionTo(TenantStatus.ENABLED).code();
        tenantDao.updateStatus(tenantId, newStatusCode);
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.TENANT, tenantId, tenantId, tenant, null));
    }

    @Override
    public PagedData<Tenant> page(PageQuery pageQuery) {
        return tenantDao.page(pageQuery,
                q -> {
                    LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<>();
                    if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
                        wrapper.like(TenantEntity::getName, q.getKeyword());
                    }
                    return wrapper;
                },
                SortField.of("createTime", SortDirection.DESC));
    }

    @Override
    public List<String> findAllIds() {
        return tenantDao.findAllIds();
    }

    private Tenant load(String id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_NOT_FOUND));
    }

    private Optional<Tenant> findById(String id) {
        return Optional.ofNullable(tenantCache.getOrLoadTransactional(id, () ->
                tenantDao.findById(id).orElse(null)
        ));
    }

    private String persistTenant(TenantCreateRequest request) {
        String tenantId = TenantIdSupport.requireCreatable(request.tenantId());
        Optional<Tenant> existing = findById(tenantId);
        if (existing != null && existing.isPresent()) {
            throw new BizException(SystemErrorCode.TENANT_CODE_EXISTS);
        }
        String tenantProfileId = resolveTenantProfileId(request.tenantProfileId());
        Tenant tenant = new Tenant(tenantId, request.name(), TenantStatus.PENDING,
                request.contactUser(), request.contactPhone(), request.expireTime(), request.remark(),
                tenantProfileId, null);
        tenantDao.create(tenant, request.adminUsername());
        return tenantId;
    }

    private String resolveTenantProfileId(String tenantProfileId) {
        if (tenantProfileId == null || tenantProfileId.isBlank()) {
            return defaultProfileId();
        }
        return tenantProfileDao.findById(tenantProfileId)
                .map(TenantProfile::getId)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    private String defaultProfileId() {
        return tenantProfileDao.findDefault()
                .map(TenantProfile::getId)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    private String requireTenantId(String id) {
        return TenantIdSupport.requireValid(id);
    }
}
