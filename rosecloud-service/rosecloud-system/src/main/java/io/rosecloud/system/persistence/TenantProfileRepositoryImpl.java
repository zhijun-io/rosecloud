package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantProfileData;
import io.rosecloud.system.domain.TenantProfileRepository;
import io.rosecloud.system.error.SystemErrorCode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TenantProfileRepositoryImpl implements TenantProfileRepository {

    private final TenantProfileMapper mapper;
    private final ObjectMapper objectMapper;

    public TenantProfileRepositoryImpl(TenantProfileMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<TenantProfile> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<TenantProfile> findDefault() {
        TenantProfileEntity po = mapper.selectOne(new LambdaQueryWrapper<TenantProfileEntity>()
                .eq(TenantProfileEntity::getIsDefault, 1));
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TenantProfile> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<TenantProfileEntity>()
                        .orderByDesc(TenantProfileEntity::getIsDefault)
                        .orderByAsc(TenantProfileEntity::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(String id) {
        return mapper.selectById(id) != null;
    }

    @Override
    public void insert(TenantProfile profile) {
        mapper.insert(toEntity(profile));
    }

    @Override
    public void update(TenantProfile profile) {
        mapper.updateById(toEntity(profile));
    }

    @Override
    public void deleteById(String id) {
        mapper.deleteById(id);
    }

    @Override
    public void makeDefault(String id) {
        // Single atomic statement so concurrent calls cannot leave all rows with
        // is_default = 0, nor produce multiple defaults.
        mapper.update(null, new LambdaUpdateWrapper<TenantProfileEntity>()
                .setSql("is_default = CASE WHEN id = ? THEN 1 ELSE 0 END", id));
    }

    @Override
    public String defaultProfileId() {
        return findDefault()
                .map(TenantProfile::getId)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    private TenantProfile toDomain(TenantProfileEntity po) {
        return new TenantProfile(po.getId(), po.getName(), po.getDescription(), po.getIsDefault(), readJson(po.getAdditionalInfo()), po.getCreateTime(), po.getCreateBy(),
                po.getUpdateTime(), po.getUpdateBy());
    }

    private TenantProfileEntity toEntity(TenantProfile profile) {
        TenantProfileEntity po = new TenantProfileEntity();
        po.setId(profile.getId());
        po.setName(profile.getName());
        po.setDescription(profile.getDescription());
        po.setAdditionalInfo(writeJson(profile.getAdditionalInfo()));
        po.setIsDefault(profile.isDefault());
        po.setCreateTime(profile.getCreateTime());
        po.setCreateBy(profile.getCreateBy());
        po.setUpdateTime(profile.getUpdateTime());
        po.setUpdateBy(profile.getUpdateBy());
        return po;
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.valueToTree(TenantProfileData.defaults());
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid tenant profile JSON", ex);
        }
    }

    private String writeJson(JsonNode value) {
        return value == null || value.isNull() ? null : value.toString();
    }
}
