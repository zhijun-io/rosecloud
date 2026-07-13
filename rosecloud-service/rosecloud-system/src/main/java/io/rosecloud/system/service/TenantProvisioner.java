package io.rosecloud.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.persistence.RoleMapper;
import io.rosecloud.system.persistence.TenantEntity;
import io.rosecloud.system.persistence.TenantMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Provisions a tenant: creates its first admin email account, grants the tenant-admin
 * role, and enables the tenant. The password is set later through the
 * authentication flow instead of being stored on the tenant record.
 */
@Component
public class TenantProvisioner {

    /** Platform-level role granted to each tenant's first admin on open. */
    private static final String TENANT_ADMIN_ROLE_CODE = "tenant-admin";

    private final TenantMapper tenantMapper;
    private final RoleMapper roleMapper;
    private final UserService userService;
    private final UserActivationService userActivationService;

    public TenantProvisioner(TenantMapper tenantMapper, RoleMapper roleMapper,
                             UserService userService, UserActivationService userActivationService) {
        this.tenantMapper = tenantMapper;
        this.roleMapper = roleMapper;
        this.userService = userService;
        this.userActivationService = userActivationService;
    }

    @Async("tenantProvisioningExecutor")
    @Transactional
    public void provision(String tenantId) {
        String adminUsername = Optional.ofNullable(tenantMapper.selectById(tenantId))
                .map(TenantEntity::getAdminUsername).orElse(null);
        if (adminUsername == null || adminUsername.isBlank()) {
            tenantMapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                    .eq(TenantEntity::getId, tenantId)
                    .set(TenantEntity::getStatus, TenantStatus.ENABLED.code()));
            return;
        }
        Role tenantAdminRole = findByCode(TENANT_ADMIN_ROLE_CODE)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
        Long userId = userService.createWithoutPassword(adminUsername, adminUsername, tenantId);
        userService.assignRoles(userId, List.of(tenantAdminRole.getId()));
        userActivationService.resend(adminUsername);
    }

    private Optional<Role> findByCode(String code) {
        return Optional.ofNullable(roleMapper.selectOne(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code))).map(this::toDomain);
    }

    private Role toDomain(RoleEntity po) {
        return new Role(po.getId(), po.getCode(), po.getName(), po.getCreateTime(), po.getCreateBy(),
                po.getUpdateTime(), po.getUpdateBy());
    }

}
