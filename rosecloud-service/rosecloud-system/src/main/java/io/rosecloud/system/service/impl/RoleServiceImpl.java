package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.security.session.LoginSessionApi;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.PagedResults;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.persistence.RoleMapper;
import io.rosecloud.system.persistence.RoleMenuEntity;
import io.rosecloud.system.persistence.RoleMenuMapper;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.persistence.UserRoleMapper;
import io.rosecloud.system.service.RoleService;
import io.rosecloud.system.service.dto.RoleCreateRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;
    private final LoginSessionApi loginSessionApi;

    private static SecurityUser currentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new BizException(SecurityErrorCode.UNAUTHORIZED);
        }
        return securityUser;
    }

    @AuditLog(action = "role-create", description = "创建角色")
    @Override
    public Long create(RoleCreateRequest request) {
        // Roles are a platform-wide catalog: a tenant admin mutating the shared role
        // definitions (code/name or its menu grants) would change permissions for every
        // tenant. Only the platform admin may define roles.
        requirePlatformAdmin();
        if (roleMapper.exists(new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, request.code()))) {
            throw new BizException(SystemErrorCode.ROLE_CODE_EXISTS);
        }
        RoleEntity po = new RoleEntity();
        po.setCode(request.code());
        po.setName(request.name());
        roleMapper.insert(po);
        return po.getId();
    }

    @Override
    public PagedData<Role> page(PageQuery pageQuery) {
        return PagedResults.page(pageQuery, RoleEntity.class, roleMapper,
                q -> {
                    LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
                    if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
                        wrapper.like(RoleEntity::getCode, q.getKeyword())
                                .or().like(RoleEntity::getName, q.getKeyword());
                    }
                    return wrapper;
                },
                SortField.of("createTime", SortDirection.DESC));
    }

    @AuditLog(action = "role-assign-menus", description = "角色菜单授权")
    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BizException(SystemErrorCode.ROLE_NOT_FOUND);
        }
        // Menu grants change the permissions carried by the role's JWT; revoke every holder's
        // sessions so the updated authority set is picked up on the next authentication.
        // Guarded to platform admin because the role catalog is shared across tenants.
        requirePlatformAdmin();
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuEntity>().eq(RoleMenuEntity::getRoleId, roleId));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                RoleMenuEntity po = new RoleMenuEntity();
                po.setRoleId(roleId);
                po.setMenuId(menuId);
                roleMenuMapper.insert(po);
            }
        }
        for (Long userId : userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getRoleId, roleId))
                .stream().map(UserRoleEntity::getUserId).toList()) {
            loginSessionApi.revokeByUserId(userId);
        }
    }

    private static void requirePlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Trusted internal service calls carry a non-SecurityUser principal (ROLE_INTERNAL) and
        // are permitted; otherwise only the platform admin (system tenant) may define roles.
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser
                && !TenantContextHolder.SYSTEM_TENANT_ID.equals(securityUser.getTenantId())) {
            throw new BizException(SecurityErrorCode.FORBIDDEN);
        }
    }

    @Override
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<RoleMenuEntity>().eq(RoleMenuEntity::getRoleId, roleId))
                .stream().map(RoleMenuEntity::getMenuId).toList();
    }

    @Override
    public Role get(Long id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
    }

    private Optional<Role> findById(Long id) {
        return Optional.ofNullable(roleMapper.selectById(id)).map(RoleEntity::toData);
    }
}
