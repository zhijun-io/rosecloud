package io.rosecloud.system.controller;

import io.rosecloud.api.user.TenantAccessCandidate;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.starter.security.annotation.InternalApi;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.TenantService;
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
@InternalApi
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
        return ApiResponse.ok(resolveAccessibleTenantIds(userId));
    }

    @GetMapping("/{userId}/tenants/candidates")
    public ApiResponse<List<TenantAccessCandidate>> listTenantCandidates(@PathVariable Long userId) {
        List<String> tenantIds = resolveAccessibleTenantIds(userId);
        List<TenantAccessCandidate> candidates = tenantIds.stream()
                .map(tenantService::get)
                .map(tenant -> toCandidate(tenant))
                .toList();
        return ApiResponse.ok(candidates);
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

    private List<String> resolveAccessibleTenantIds(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        List<String> tenantIds = (user != null && TenantContextHolder.SYSTEM_TENANT_ID.equals(user.getTenantId()))
                ? tenantService.findAllIds()
                : userRepository.findTenantIdsByUserId(userId);
        String homeTenantId = (user == null) ? null : user.getTenantId();
        if (homeTenantId == null || homeTenantId.isBlank()) {
            return tenantIds;
        }
        if (tenantIds.contains(homeTenantId)) {
            return tenantIds;
        }
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(homeTenantId), tenantIds.stream())
                .distinct()
                .toList();
    }

    private static TenantAccessCandidate toCandidate(Tenant tenant) {
        // ENABLED: full access. EXPIRED (停用): switchable with write restrictions.
        // DISABLED and PENDING: never selectable — blocked at auth time.
        boolean selectable = (tenant.getStatus() == TenantStatus.ENABLED)
                || (tenant.getStatus() == TenantStatus.EXPIRED);
        return new TenantAccessCandidate(tenant.getId(), tenant.getName(), tenant.getStatus().name(), selectable);
    }
}
