package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.user.TenantLookupApi;
import io.rosecloud.common.security.user.TenantStatusView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "rosecloud-system", contextId = "tenantLookupApi", path = "/api/internal/tenants")
public interface TenantLookupFeignApi extends TenantLookupApi {

    @Override
    @GetMapping("/{tenantId}")
    ApiResponse<TenantStatusView> findTenantStatus(@PathVariable("tenantId") String tenantId);
}
