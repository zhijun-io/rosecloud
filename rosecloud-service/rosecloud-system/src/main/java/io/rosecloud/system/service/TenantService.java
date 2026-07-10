package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.service.dto.TenantCreateRequest;
import io.rosecloud.system.service.dto.TenantUpdateRequest;

public interface TenantService {

    String create(TenantCreateRequest request);

    void update(String id, TenantUpdateRequest request);

    void delete(String id);

    Tenant get(String id);

    String open(String id);

    void disable(String id);

    void enable(String id);

    PageResult<Tenant> page(long current, long size, String keyword);

    /** All tenant ids (excluding the reserved system tenant). */
    List<String> findAllIds();
}
