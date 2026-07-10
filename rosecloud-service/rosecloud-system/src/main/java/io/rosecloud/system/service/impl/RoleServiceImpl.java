package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.RoleRepository;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.RoleService;
import io.rosecloud.system.service.dto.RoleCreateRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SessionStore sessionStore;

    public RoleServiceImpl(RoleRepository roleRepository, UserRepository userRepository, SessionStore sessionStore) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.sessionStore = sessionStore;
    }

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
        if (roleRepository.existsByCode(request.code())) {
            throw new BizException(SystemErrorCode.ROLE_CODE_EXISTS);
        }
        return roleRepository.insert(new Role(null, request.code(), request.name()));
    }

    @Override
    public PageResult<Role> page(long current, long size, String keyword) {
        return roleRepository.page(current, size, keyword);
    }
    @AuditLog(action = "role-assign-menus", description = "角色菜单授权")
    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (!roleRepository.existsById(roleId)) {
            throw new BizException(SystemErrorCode.ROLE_NOT_FOUND);
        }
        // Menu grants change the permissions carried by the role's JWT; revoke every holder's
        // sessions so the updated authority set is picked up on the next authentication.
        // Guarded to platform admin because the role catalog is shared across tenants.
        requirePlatformAdmin();
        roleRepository.assignMenus(roleId, menuIds == null ? List.of() : menuIds);
        for (Long userId : userRepository.findUserIdsByRoleId(roleId)) {
            sessionStore.revokeByUserId(userId);
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
        return roleRepository.findMenuIdsByRoleId(roleId);
    }
    @Override
    public Role get(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
    }
}
