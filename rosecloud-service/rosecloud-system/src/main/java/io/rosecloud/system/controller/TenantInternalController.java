package io.rosecloud.system.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.api.user.TenantLookupApi;
import io.rosecloud.api.user.TenantStatusView;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.starter.security.annotation.InternalApi;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.service.TenantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@InternalApi
@RequestMapping(ServiceMetadata.API_PREFIX + "/internal/tenants")
public class TenantInternalController implements TenantLookupApi {

    private final TenantService tenantService;
    @GetMapping("/{tenantId}")
    public ApiResponse<TenantStatusView> findTenantStatus(@PathVariable String tenantId) {
        Tenant tenant = tenantService.get(tenantId);
        return ApiResponse.ok(new TenantStatusView(tenant.getId(), tenant.getStatus().name()));
    }
}
