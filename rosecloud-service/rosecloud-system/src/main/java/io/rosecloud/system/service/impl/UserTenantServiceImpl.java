package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.api.user.TenantAccessCandidate;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.persistence.UserEntity;
import io.rosecloud.system.persistence.UserMapper;
import io.rosecloud.system.persistence.UserTenantEntity;
import io.rosecloud.system.persistence.UserTenantMapper;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.UserTenantService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class UserTenantServiceImpl implements UserTenantService {

    private final UserMapper userMapper;
    private final UserTenantMapper userTenantMapper;
    private final TenantService tenantService;

    @Override
    public List<String> listTenantIds(Long userId) {
        return resolveAccessibleTenantIds(userId);
    }

    @Override
    public List<TenantAccessCandidate> listTenantCandidates(Long userId) {
        List<String> tenantIds = resolveAccessibleTenantIds(userId);
        return tenantIds.stream()
                .map(tenantService::get)
                .map(this::toCandidate)
                .toList();
    }

    @Override
    public boolean isPlatformAdmin(Long userId) {
        return findById(userId)
                .map(user -> TenantContextHolder.SYSTEM_TENANT_ID.equals(user.getTenantId()))
                .orElse(false);
    }

    // ==================== private helpers ====================

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
        return Stream.concat(Stream.of(homeTenantId), tenantIds.stream())
                .distinct()
                .toList();
    }

    private Optional<User> findById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId)).map(UserEntity::toData);
    }

    private List<String> findTenantIdsByUserId(Long userId) {
        return userTenantMapper.selectList(
                        new LambdaQueryWrapper<UserTenantEntity>().eq(UserTenantEntity::getUserId, userId))
                .stream().map(UserTenantEntity::getTenantId).toList();
    }

    private TenantAccessCandidate toCandidate(Tenant tenant) {
        // ENABLED: full access. EXPIRED (停用): switchable with write restrictions.
        // DISABLED and PENDING: never selectable — blocked at auth time.
        boolean selectable = (tenant.getStatus() == TenantStatus.ENABLED)
                || (tenant.getStatus() == TenantStatus.EXPIRED);
        return new TenantAccessCandidate(tenant.getId(), tenant.getName(), tenant.getStatus().name(), selectable);
    }
}
