package io.rosecloud.system.controller;
import lombok.RequiredArgsConstructor;

import io.rosecloud.api.user.TenantAccessCandidate;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.starter.security.annotation.InternalApi;
import io.rosecloud.system.service.UserTenantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal (service-to-service) endpoint backing tenant-switch support in the auth service.
 * Guarded by {@code @InternalApi} on the consumer contract; reachable only from inside the
 * platform. Never exposed to external clients.
 */
@RequiredArgsConstructor
@RestController
@InternalApi
@RequestMapping(ServiceMetadata.API_PREFIX + "/internal/user-tenants")
public class UserTenantController {

    private final UserTenantService userTenantService;

    @GetMapping("/{userId}/tenants")
    public ApiResponse<List<String>> listTenantIds(@PathVariable Long userId) {
        return ApiResponse.ok(userTenantService.listTenantIds(userId));
    }

    @GetMapping("/{userId}/tenants/candidates")
    public ApiResponse<List<TenantAccessCandidate>> listTenantCandidates(@PathVariable Long userId) {
        return ApiResponse.ok(userTenantService.listTenantCandidates(userId));
    }

    @GetMapping("/{userId}/platform-admin")
    public ApiResponse<Boolean> isPlatformAdmin(@PathVariable Long userId) {
        return ApiResponse.ok(userTenantService.isPlatformAdmin(userId));
    }

}
