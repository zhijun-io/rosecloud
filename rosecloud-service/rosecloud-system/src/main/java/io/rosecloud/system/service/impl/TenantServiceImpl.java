package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantMapper tenantMapper;
    private final TenantProfileMapper tenantProfileMapper;
    private final ObjectMapper objectMapper;
    private final TenantProvisioner tenantProvisioner;

    public TenantServiceImpl(TenantMapper tenantMapper, TenantProfileMapper tenantProfileMapper,
                             ObjectMapper objectMapper, TenantProvisioner tenantProvisioner) {
        this.tenantMapper = tenantMapper;
        this.tenantProfileMapper = tenantProfileMapper;
        this.objectMapper = objectMapper;
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
    }

    @AuditLog(action = "tenant-delete", description = "删除租户")
    @Override
    public void delete(String id) {
        String tenantId = requireTenantId(id);
        load(tenantId);
        tenantMapper.deleteById(tenantId);
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
    }

    @Override
    public PageResult<Tenant> page(long current, long size, String keyword) {
        Page<TenantEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(TenantEntity::getName, keyword);
        }
        wrapper.orderByDesc(TenantEntity::getCreateTime);
        IPage<TenantEntity> result = tenantMapper.selectPage(page, wrapper);
        List<Tenant> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
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
        return Optional.ofNullable(tenantMapper.selectById(id)).map(this::toDomain);
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
        TenantEntity po = toEntity(tenant);
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

    private Tenant toDomain(TenantEntity po) {
        return new Tenant(po.getId(), po.getName(),
                resolveStatus(po.getStatus(), po.getExpireTime()), po.getContactUser(), po.getContactPhone(),
                po.getExpireTime(), po.getRemark(), po.getTenantProfileId(), readJson(po.getExtra()),
                po.getCreateTime(), po.getCreateBy(), po.getUpdateTime(), po.getUpdateBy());
    }

    private TenantStatus resolveStatus(Integer status, LocalDate expireTime) {
        if (status == null) {
            return null;
        }
        if (expireTime != null && expireTime.isBefore(LocalDate.now())) {
            return TenantStatus.EXPIRED;
        }
        return TenantStatus.of(status);
    }

    private TenantEntity toEntity(Tenant t) {
        TenantEntity po = new TenantEntity();
        po.setId(t.getId());
        po.setName(t.getName());
        po.setStatus(t.getStatus() == null ? null : t.getStatus().code());
        po.setContactUser(t.getContactUser());
        po.setContactPhone(t.getContactPhone());
        po.setExpireTime(t.getExpireTime());
        po.setRemark(t.getRemark());
        po.setTenantProfileId(t.getTenantProfileId());
        po.setExtra(writeJson(t.getAdditionalInfo()));
        po.setCreateTime(t.getCreateTime());
        po.setCreateBy(t.getCreateBy());
        po.setUpdateTime(t.getUpdateTime());
        po.setUpdateBy(t.getUpdateBy());
        return po;
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid tenant extra JSON", ex);
        }
    }

    private String writeJson(JsonNode value) {
        return value == null || value.isNull() ? null : value.toString();
    }
}
