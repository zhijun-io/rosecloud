package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantEntity;
import io.rosecloud.system.persistence.TenantMapper;
import io.rosecloud.system.persistence.TenantProfileEntity;
import io.rosecloud.system.persistence.TenantProfileMapper;
import io.rosecloud.system.service.TenantProfileService;
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TenantProfileServiceImpl implements TenantProfileService {

    private final TenantProfileMapper tenantProfileMapper;
    private final TenantMapper tenantMapper;

    public TenantProfileServiceImpl(TenantProfileMapper tenantProfileMapper, TenantMapper tenantMapper) {
        this.tenantProfileMapper = tenantProfileMapper;
        this.tenantMapper = tenantMapper;
    }

    @Transactional
    @Override
    public String create(TenantProfileCreateRequest request) {
        String id = requireId(request.id());
        if (tenantProfileMapper.selectById(id) != null) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_EXISTS);
        }
        TenantProfile profile = new TenantProfile(id, request.name(), request.description(), request.profileData());
        tenantProfileMapper.insert(new TenantProfileEntity().toEntity(profile));
        return id;
    }

    @Transactional
    @Override
    public void update(String id, TenantProfileUpdateRequest request) {
        load(id);
        TenantProfile profile = new TenantProfile(id, request.name(), request.description(), request.profileData());
        tenantProfileMapper.updateById(new TenantProfileEntity().toEntity(profile));
    }

    @Transactional
    @Override
    public void delete(String id) {
        TenantProfile profile = load(id);
        if (profile.isDefault()) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_DEFAULT_DELETE_FORBIDDEN);
        }
        if (tenantMapper.selectCount(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getTenantProfileId, id)) > 0) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_IN_USE);
        }
        tenantProfileMapper.deleteById(id);
    }

    @Transactional
    @Override
    public void makeDefault(String id) {
        load(id);
        tenantProfileMapper.update(null, new LambdaUpdateWrapper<TenantProfileEntity>()
                .setSql("is_default = CASE WHEN id = {0} THEN 1 ELSE 0 END", id));
    }

    @Override
    public TenantProfile get(String id) {
        return load(id);
    }

    @Override
    public TenantProfile getDefault() {
        return findDefault()
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    @Override
    public List<TenantProfile> list() {
        return tenantProfileMapper.selectList(new LambdaQueryWrapper<TenantProfileEntity>()
                        .orderByDesc(TenantProfileEntity::getIsDefault)
                        .orderByAsc(TenantProfileEntity::getId))
                .stream()
                .map(TenantProfileEntity::toData)
                .toList();
    }

    private TenantProfile load(String id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    private Optional<TenantProfile> findById(String id) {
        return Optional.ofNullable(tenantProfileMapper.selectById(id)).map(TenantProfileEntity::toData);
    }

    private Optional<TenantProfile> findDefault() {
        TenantProfileEntity po = tenantProfileMapper.selectOne(new LambdaQueryWrapper<TenantProfileEntity>()
                .eq(TenantProfileEntity::getIsDefault, 1));
        return Optional.ofNullable(po).map(TenantProfileEntity::toData);
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_ID_REQUIRED);
        }
        return id;
    }
}
