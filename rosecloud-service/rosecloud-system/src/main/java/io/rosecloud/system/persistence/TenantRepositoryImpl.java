package io.rosecloud.system.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantMapper mapper;
    private final ObjectMapper objectMapper;

    public TenantRepositoryImpl(TenantMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Tenant> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public String insert(Tenant tenant, String adminUsername) {
        TenantEntity po = toEntity(tenant);
        po.setAdminUsername(adminUsername);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public Optional<String> findAdminUsername(String id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(TenantEntity::getAdminUsername);
    }

    @Override
    public void updateStatus(String id, TenantStatus status) {
        mapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, id)
                .set(TenantEntity::getStatus, status.code()));
    }

    @Override
    public void update(Tenant tenant) {
        mapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, tenant.getId())
                .set(TenantEntity::getName, tenant.getName())
                .set(TenantEntity::getStatus, tenant.getStatus() == null ? null : tenant.getStatus().code())
                .set(TenantEntity::getContactUser, tenant.getContactUser())
                .set(TenantEntity::getContactPhone, tenant.getContactPhone())
                .set(TenantEntity::getExpireTime, tenant.getExpireTime())
                .set(TenantEntity::getRemark, tenant.getRemark())
                .set(TenantEntity::getTenantProfileId, tenant.getTenantProfileId())
                .set(TenantEntity::getExtra, writeJson(tenant.getAdditionalInfo())));
    }

    @Override
    public void deleteById(String id) {
        mapper.deleteById(id);
    }

    @Override
    public long countByTenantProfileId(String tenantProfileId) {
        return mapper.selectCount(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getTenantProfileId, tenantProfileId));
    }

    @Override
    public PageResult<Tenant> page(long current, long size, String keyword) {
        Page<TenantEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(TenantEntity::getName, keyword);
        }
        wrapper.orderByDesc(TenantEntity::getCreateTime);
        IPage<TenantEntity> result = mapper.selectPage(page, wrapper);
        List<Tenant> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Tenant toDomain(TenantEntity po) {
        return new Tenant(po.getId(), po.getName(),
                resolveStatus(po.getStatus(), po.getExpireTime()), po.getContactUser(), po.getContactPhone(),
                po.getExpireTime(), po.getRemark(), po.getTenantProfileId(), readJson(po.getExtra()));
    }

    private TenantStatus resolveStatus(Integer status, java.time.LocalDate expireTime) {
        if (status == null) {
            return null;
        }
        if (expireTime != null && expireTime.isBefore(java.time.LocalDate.now())) {
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
