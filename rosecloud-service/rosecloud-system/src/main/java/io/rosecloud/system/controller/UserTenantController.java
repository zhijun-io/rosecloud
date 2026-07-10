package io.rosecloud.system.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.TenantService;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/internal/user-tenants")
public class UserTenantController {

    private final UserRepository userRepository;
    private final TenantService tenantService;

    public UserTenantController(UserRepository userRepository, TenantService tenantService) {
        this.userRepository = userRepository;
        this.tenantService = tenantService;
    }

    @GetMapping("/{userId}/tenants")
    public ApiResponse<List<String>> listTenantIds(@PathVariable Long userId) {
        if (isSystemTenantAdmin(userId)) {
            // Platform admins can switch into any tenant.
            return ApiResponse.ok(tenantService.findAllIds());
        }
        return ApiResponse.ok(userRepository.findTenantIdsByUserId(userId));
    }

    @GetMapping("/{userId}/platform-admin")
    public ApiResponse<Boolean> isPlatformAdmin(@PathVariable Long userId) {
        return ApiResponse.ok(isSystemTenantAdmin(userId));
    }

    private boolean isSystemTenantAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> TenantContextHolder.SYSTEM_TENANT_ID.equals(user.getTenantId()))
                .orElse(false);
    }
}
