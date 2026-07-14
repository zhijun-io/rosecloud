package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantDao;
import io.rosecloud.system.persistence.TenantProfileDao;
import io.rosecloud.system.service.TenantProfileService;
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class TenantProfileServiceImpl implements TenantProfileService {

    private final TenantProfileDao tenantProfileDao;
    private final TenantDao tenantDao;

    @Transactional
    @Override
    public String create(TenantProfileCreateRequest request) {
        String id = requireId(request.id());
        if (tenantProfileDao.findById(id).isPresent()) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_EXISTS);
        }
        TenantProfile profile = new TenantProfile(id, request.name(), request.description(), request.profileData());
        tenantProfileDao.save(profile);
        return id;
    }

    @Transactional
    @Override
    public void update(String id, TenantProfileUpdateRequest request) {
        load(id);
        TenantProfile profile = new TenantProfile(id, request.name(), request.description(), request.profileData());
        tenantProfileDao.save(profile);
    }

    @Transactional
    @Override
    public void delete(String id) {
        TenantProfile profile = load(id);
        if (profile.isDefault()) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_DEFAULT_DELETE_FORBIDDEN);
        }
        if (tenantDao.countByProfileId(id) > 0) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_IN_USE);
        }
        tenantProfileDao.removeById(id);
    }

    @Transactional
    @Override
    public void makeDefault(String id) {
        load(id);
        tenantProfileDao.makeDefault(id);
    }

    @Override
    public TenantProfile get(String id) {
        return load(id);
    }

    @Override
    public TenantProfile getDefault() {
        return tenantProfileDao.findDefault()
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    @Override
    public List<TenantProfile> list() {
        return tenantProfileDao.findAllOrdered();
    }

    private TenantProfile load(String id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_PROFILE_NOT_FOUND));
    }

    private Optional<TenantProfile> findById(String id) {
        return tenantProfileDao.findById(id);
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new BizException(SystemErrorCode.TENANT_PROFILE_ID_REQUIRED);
        }
        return id;
    }
}
