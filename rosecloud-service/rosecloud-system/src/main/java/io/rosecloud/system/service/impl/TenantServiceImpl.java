package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.PagedResults;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantEntity;
import io.rosecloud.system.persistence.TenantMapper;
import io.rosecloud.system.persistence.TenantProfileEntity;
import io.rosecloud.system.persistence.TenantProfileMapper;
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

    private final TenantMapper tenantMapper;
    private final TenantProfileMapper tenantProfileMapper;
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
        tenantMapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, updated.getId())
                .set(TenantEntity::getName, updated.getName())
                .set(TenantEntity::getStatus, updated.getStatus() == null ? null : updated.getStatus().code())
                .set(TenantEntity::getContactUser, updated.getContactUser())
                .set(TenantEntity::getContactPhone, updated.getContactPhone())
                .set(TenantEntity::getExpireTime, updated.getExpireTime())
                .set(TenantEntity::getRemark, updated.getRemark())
                .set(TenantEntity::getTenantProfileId, updated.getTenantProfileId())
                .set(TenantEntity::getExtra, writeJson(updated.getAdditionalInfo())));
        tenantCache.evict(tenantId);
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
        tenantMapper.deleteById(tenantId);
        tenantCache.evict(tenantId);
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
        tenantMapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, tenantId)
                .set(TenantEntity::getStatus,
                        tenant.getStatus().transitionTo(TenantStatus.DISABLED).code()));
        tenantCache.evict(tenantId);
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
        tenantMapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, tenantId)
                .set(TenantEntity::getStatus,
                        tenant.getStatus().transitionTo(TenantStatus.ENABLED).code()));
        tenantCache.evict(tenantId);
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.TENANT, tenantId, tenantId, tenant, null));
    }

    @Override
    public PagedData<Tenant> page(PageQuery pageQuery) {
        return PagedResults.page(pageQuery, TenantEntity.class, tenantMapper,
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
        return tenantMapper.selectList(new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getDeleted, 0)
                        .ne(TenantEntity::getId, TenantContextHolder.SYSTEM_TENANT_ID))
                .stream().map(TenantEntity::getId).toList();
    }

    private Tenant load(String id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_NOT_FOUND));
    }

    private Optional<Tenant> findById(String id) {
        Tenant cached = tenantCache.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        return Optional.ofNullable(tenantMapper.selectById(id)).map(po -> {
            Tenant t = po.toData();
            tenantCache.put(id, t);
            return t;
        });
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
        TenantEntity po = new TenantEntity().toEntity(tenant);
        po.setAdminUsername(request.adminUsername());
        tenantMapper.insert(po);
        return tenantId;
    }

    private String resolveTenantProfileId(String tenantProfileId) {
        if (tenantProfileId == null || tenantProfileId.isBlank()) {
            return defaultProfileId();
        }
        if (tenantProfileMapper.selectById(tenantProfileId) == null) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND);
        }
        return tenantProfileId;
    }

    private String defaultProfileId() {
        TenantProfileEntity def = tenantProfileMapper.selectOne(
                new LambdaQueryWrapper<TenantProfileEntity>().eq(TenantProfileEntity::getIsDefault, 1));
        if (def == null) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND);
        }
        return def.getId();
    }

    private String requireTenantId(String id) {
        return TenantIdSupport.requireValid(id);
    }

    private String writeJson(JsonNode value) {
        return value == null || value.isNull() ? null : value.toString();
    }
}
