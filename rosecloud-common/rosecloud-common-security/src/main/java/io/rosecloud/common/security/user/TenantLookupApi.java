package io.rosecloud.common.security.user;

import io.rosecloud.common.core.model.ApiResponse;

/**
 * Lookup interface for tenant status. Implemented by the system service and consumed by
 * the security starter so it can validate tenant state without a direct dependency on
 * {@code rosecloud-api}.
 */
public interface TenantLookupApi {

    ApiResponse<TenantStatusView> findTenantStatus(String tenantId);
}
