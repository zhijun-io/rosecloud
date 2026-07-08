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
import io.rosecloud.system.domain.TenantAdminCredentials;
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
    public Optional<Tenant> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return mapper.exists(new LambdaQueryWrapper<TenantEntity>().eq(TenantEntity::getCode, code));
    }

    @Override
    public Long insert(Tenant tenant, String adminUsername, String adminPasswordHash) {
        TenantEntity po = toEntity(tenant);
        po.setId(null);
        po.setAdminUsername(adminUsername);
        po.setAdminPassword(adminPasswordHash);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public Optional<TenantAdminCredentials> findAdminCredentials(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(po -> new TenantAdminCredentials(po.getAdminUsername(), po.getAdminPassword()));
    }

    @Override
    public void clearAdminPassword(Long id) {
        mapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, id)
                .set(TenantEntity::getAdminPassword, null));
    }

    @Override
    public void updateStatus(Long id, TenantStatus status) {
        mapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, id)
                .set(TenantEntity::getStatus, status.code()));
    }

    @Override
    public PageResult<Tenant> page(long current, long size, String keyword) {
        Page<TenantEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(TenantEntity::getName, keyword).or().like(TenantEntity::getCode, keyword);
        }
        wrapper.orderByDesc(TenantEntity::getCreateTime);
        IPage<TenantEntity> result = mapper.selectPage(page, wrapper);
        List<Tenant> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Tenant toDomain(TenantEntity po) {
        return new Tenant(po.getId(), po.getName(), po.getCode(),
                resolveStatus(po.getStatus(), po.getExpireTime()), po.getContactUser(), po.getContactPhone(),
                po.getExpireTime(), po.getRemark(), readJson(po.getExtra()));
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
        po.setCode(t.getCode());
        po.setStatus(t.getStatus() == null ? null : t.getStatus().code());
        po.setContactUser(t.getContactUser());
        po.setContactPhone(t.getContactPhone());
        po.setExpireTime(t.getExpireTime());
        po.setRemark(t.getRemark());
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
