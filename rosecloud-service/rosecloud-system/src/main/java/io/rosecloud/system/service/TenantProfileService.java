package io.rosecloud.system.service;

import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;

import java.util.List;

public interface TenantProfileService {

    String create(TenantProfileCreateRequest request);

    void update(String id, TenantProfileUpdateRequest request);

    void delete(String id);

    void makeDefault(String id);

    TenantProfile get(String id);

    TenantProfile getDefault();

    List<TenantProfile> list();
}
