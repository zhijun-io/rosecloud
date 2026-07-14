package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.starter.security.session.LoginSessionApi;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.RoleDao;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.service.RoleService;
import io.rosecloud.system.service.dto.RoleCreateRequest;
import io.rosecloud.system.service.validator.RoleValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;

@RequiredArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleDao roleDao;
    private final RoleValidator roleValidator;
    private final LoginSessionApi loginSessionApi;

    // ==== 缓存 ====
    private final EntityCache<Long, List<Long>> roleMenuIdsCache;
    private final EntityCache<String, List<Menu>> menuListCache;
    private final EntityEventPublisher eventPublisher;

    @AuditLog(action = "role-create", description = "创建角色")
    @Override
    public Long create(RoleCreateRequest request) {
        requirePlatformAdmin();
        Role role = new Role(null, request.code(), request.name(), null, null, null, null);
        roleValidator.validateCreate(role);
        Role saved = roleDao.save(role);
        eventPublisher.publish(EntityChangedEvent.created(EntityCacheNames.ROLE, saved.getId(),
                TenantContextHolder.getTenantId(), null));
        return saved.getId();
    }

    @Override
    public PagedData<Role> page(PageQuery pageQuery) {
        return roleDao.page(pageQuery,
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
        roleDao.findById(roleId).orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
        requirePlatformAdmin();
        roleDao.deleteRoleMenuByRoleId(roleId);
        roleDao.assignMenusToRole(roleId, menuIds);
        // 角色菜单授权变更：失效该角色菜单 ID 缓存，以及按角色组合键缓存的菜单列表。
        roleMenuIdsCache.evict(roleId);
        menuListCache.evictAll();
        // 吊销所有持有该角色的用户会话，确保下次登录获得最新的权限集合。
        for (Long userId : roleDao.findUserIdsByRoleId(roleId)) {
            loginSessionApi.revokeByUserId(userId);
        }

        eventPublisher.publish(EntityChangedEvent.updated(EntityCacheNames.ROLE, roleId,
                TenantContextHolder.getTenantId(), null, null));
    }

    private static void requirePlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser
                && !TenantContextHolder.SYSTEM_TENANT_ID.equals(securityUser.getTenantId())) {
            throw new BizException(SecurityErrorCode.FORBIDDEN);
        }
    }

    /**
     * 查询角色关联的菜单 ID 列表。结果缓存于 roleMenuIdsCache，
     * 在 assignMenus 对该角色重新授权时失效。
     */
    @Override
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return roleMenuIdsCache.getOrLoad(roleId, () -> roleDao.findMenuIdsByRoleId(roleId));
    }

    @Override
    public Role get(Long id) {
        return roleDao.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
    }
}
