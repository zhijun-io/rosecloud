package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantProfileData;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantProfileRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.TenantProfileService;
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantProfileServiceImpl implements TenantProfileService {

    private final TenantProfileRepository tenantProfileRepository;
    private final TenantRepository tenantRepository;

    public TenantProfileServiceImpl(TenantProfileRepository tenantProfileRepository, TenantRepository tenantRepository) {
        this.tenantProfileRepository = tenantProfileRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    @Override
    public String create(TenantProfileCreateRequest request) {
        String id = requireId(request.id());
        if (tenantProfileRepository.existsById(id)) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_EXISTS);
        }
        TenantProfile profile = new TenantProfile(id, request.name(), request.description(), request.profileData());
        tenantProfileRepository.insert(profile);
        return id;
    }

    @Transactional
    @Override
    public void update(String id, TenantProfileUpdateRequest request) {
        load(id);
        TenantProfile profile = new TenantProfile(id, request.name(), request.description(), request.profileData());
        tenantProfileRepository.update(profile);
    }

    @Transactional
    @Override
    public void delete(String id) {
        TenantProfile profile = load(id);
        if (profile.isDefault()) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_DEFAULT_DELETE_FORBIDDEN);
        }
        if (tenantRepository.countByTenantProfileId(id) > 0) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_IN_USE);
        }
        tenantProfileRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void makeDefault(String id) {
        load(id);
        tenantProfileRepository.makeDefault(id);
    }

    @Override
    public TenantProfile get(String id) {
        return load(id);
    }

    @Override
    public TenantProfile getDefault() {
        return tenantProfileRepository.findDefault()
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    @Override
    public List<TenantProfile> list() {
        return tenantProfileRepository.findAll();
    }

    private TenantProfile load(String id) {
        return tenantProfileRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_ID_REQUIRED);
        }
        return id;
    }
}
