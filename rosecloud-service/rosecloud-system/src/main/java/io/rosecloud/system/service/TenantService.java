package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.service.dto.TenantCreateRequest;
import io.rosecloud.system.service.dto.TenantUpdateRequest;

import java.util.List;

public interface TenantService {

    String create(TenantCreateRequest request);

    void update(String id, TenantUpdateRequest request);

    void delete(String id);

    Tenant get(String id);

    String open(String id);

    void disable(String id);

    void enable(String id);

    PagedData<Tenant> page(PageQuery pageQuery);

    /** All tenant ids (excluding the reserved system tenant). */
    List<String> findAllIds();
}
