package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.service.dto.TenantApplyRequest;

public interface TenantService {

    Long apply(TenantApplyRequest request);

    Long open(Long id);

    void disable(Long id);

    void enable(Long id);

    PageResult<Tenant> page(long current, long size, String keyword);
}
