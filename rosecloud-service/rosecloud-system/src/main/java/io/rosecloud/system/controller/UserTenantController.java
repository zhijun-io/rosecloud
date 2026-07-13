package io.rosecloud.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.api.user.TenantAccessCandidate;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.starter.security.annotation.InternalApi;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.persistence.UserEntity;
import io.rosecloud.system.persistence.UserMapper;
import io.rosecloud.system.persistence.UserTenantEntity;
import io.rosecloud.system.persistence.UserTenantMapper;
import io.rosecloud.system.service.TenantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Internal (service-to-service) endpoint backing tenant-switch support in the auth service.
 * Guarded by {@code @InternalApi} on the consumer contract; reachable only from inside the
 * platform. Never exposed to external clients.
 */
@RestController
@InternalApi
@RequestMapping(ServiceMetadata.API_PREFIX + "/internal/user-tenants")
public class UserTenantController {

    private final UserMapper userMapper;
    private final UserTenantMapper userTenantMapper;
    private final TenantService tenantService;
    private final ObjectMapper objectMapper;

    public UserTenantController(UserMapper userMapper, UserTenantMapper userTenantMapper,
                                TenantService tenantService, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.userTenantMapper = userTenantMapper;
        this.tenantService = tenantService;
        this.objectMapper = objectMapper;
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
        return findById(userId)
                .map(user -> TenantContextHolder.SYSTEM_TENANT_ID.equals(user.getTenantId()))
                .orElse(false);
    }

    private List<String> resolveAccessibleTenantIds(Long userId) {
        User user = findById(userId).orElse(null);
        List<String> tenantIds = (user != null && TenantContextHolder.SYSTEM_TENANT_ID.equals(user.getTenantId()))
                ? tenantService.findAllIds()
                : findTenantIdsByUserId(userId);
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

    private Optional<User> findById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId)).map(this::toDomain);
    }

    private List<String> findTenantIdsByUserId(Long userId) {
        return userTenantMapper.selectList(
                        new LambdaQueryWrapper<UserTenantEntity>().eq(UserTenantEntity::getUserId, userId))
                .stream().map(UserTenantEntity::getTenantId).toList();
    }

    private static TenantAccessCandidate toCandidate(Tenant tenant) {
        // ENABLED: full access. EXPIRED (停用): switchable with write restrictions.
        // DISABLED and PENDING: never selectable — blocked at auth time.
        boolean selectable = (tenant.getStatus() == TenantStatus.ENABLED)
                || (tenant.getStatus() == TenantStatus.EXPIRED);
        return new TenantAccessCandidate(tenant.getId(), tenant.getName(), tenant.getStatus().name(), selectable);
    }

    private User toDomain(UserEntity po) {
        return new User(po.getId(), loginName(po), po.getNickname(), po.getStatus(), po.getTenantId(),
                readJson(po.getAdditionalInfo()), po.getCreateTime(), po.getCreateBy(), po.getUpdateTime(),
                po.getUpdateBy());
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid user extra JSON", ex);
        }
    }

    private String loginName(UserEntity user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        return null;
    }
}
