package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.service.dto.TenantApplyRequest;

public interface TenantService {

    String apply(TenantApplyRequest request);

    Tenant get(String id);

    String open(String id);

    void disable(String id);

    void enable(String id);

    PageResult<Tenant> page(long current, long size, String keyword);
}
