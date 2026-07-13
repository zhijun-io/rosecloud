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
import io.rosecloud.system.persistence.UserCredentialEntity;
import io.rosecloud.system.persistence.UserCredentialMapper;
import io.rosecloud.system.persistence.UserEntity;
import io.rosecloud.system.persistence.UserMapper;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.persistence.UserRoleMapper;
import io.rosecloud.system.persistence.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages tenant lifecycle — provisioning and deprovisioning.
 *
 * <p>Provisioning creates the first admin account, grants the tenant-admin
 * role, and enables the tenant. Deprovisioning cascade-deletes all tenant-scoped
 * resources (users, credentials, role links).
 *
 * <p>借鉴 ThingsBoard {@code TenantServiceImpl.deleteTenant()} 的级联清理模式
 * 和 {@code PaginatedRemover} 分批删除思想。
 */
@Component
@RequiredArgsConstructor
public class TenantProvisioner {

    /** Platform-level role granted to each tenant's first admin on open. */
    private static final String TENANT_ADMIN_ROLE_CODE = "tenant-admin";

    private final TenantMapper tenantMapper;
    private final RoleMapper roleMapper;
    private final UserService userService;
    private final UserActivationService userActivationService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserCredentialMapper userCredentialMapper;

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

    /**
     * 级联清理租户下的所有资源。
     *
     * <p>删除顺序：activation credentials → user-role links → users。
     * 借鉴 ThingsBoard {@code TenantServiceImpl.deleteTenant()} 先清理关联实体后删除自身的顺序。
     *
     * <p>当前使用单条 DELETE 语句；当单租户用户数超 1 万时，可改用
     * {@link io.rosecloud.starter.data.PaginatedRemover} 分批删除。
     */
    @Transactional
    public void deprovision(String tenantId) {
        List<Long> userIds = userMapper.selectList(
                        new LambdaQueryWrapper<UserEntity>()
                                .eq(UserEntity::getTenantId, tenantId))
                .stream().map(UserEntity::getId).toList();

        if (userIds.isEmpty()) {
            return;
        }

        userCredentialMapper.delete(
                new LambdaQueryWrapper<UserCredentialEntity>()
                        .in(UserCredentialEntity::getUserId, userIds));

        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRoleEntity>()
                        .in(UserRoleEntity::getUserId, userIds));

        userMapper.delete(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getTenantId, tenantId));
    }


    private Optional<Role> findByCode(String code) {
        return Optional.ofNullable(roleMapper.selectOne(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code))).map(RoleEntity::toData);
    }

}
