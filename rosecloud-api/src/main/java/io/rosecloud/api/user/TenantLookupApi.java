package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;

public interface TenantLookupApi {

    ApiResponse<TenantStatusView> findTenantStatus(String tenantId);
}
