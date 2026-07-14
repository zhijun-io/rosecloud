package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.domain.MenuType;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.MenuDao;
import io.rosecloud.system.service.MenuService;
import io.rosecloud.system.service.dto.MenuRequest;
import io.rosecloud.system.service.dto.MenuTreeNode;
import io.rosecloud.system.service.dto.UserMenuResult;
import io.rosecloud.system.service.validator.MenuValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MenuServiceImpl implements MenuService {

    private final MenuDao menuDao;
    private final MenuValidator menuValidator;
    private final EntityCache<Long, Menu> menuCache;
    private final EntityCache<String, List<Menu>> menuListCache;
    private final EntityCache<Long, List<Long>> roleMenuIdsCache;
    private final EntityEventPublisher eventPublisher;

    @AuditLog(action = "menu-create", description = "创建菜单")
    @Transactional
    @Override
    public Long create(MenuRequest request) {
        Menu menu = toMenu(null, request);
        menuValidator.validateCreate(menu);
        Menu saved = menuDao.save(menu);
        menuListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.created(
                EntityCacheNames.MENU, saved.getId(), null, null));
        return saved.getId();
    }

    @AuditLog(action = "menu-update", description = "修改菜单")
    @Transactional
    @Override
    public void update(Long id, MenuRequest request) {
        Menu existing = findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.MENU_NOT_FOUND));
        Menu updated = toMenu(id, request);
        menuValidator.validateUpdate(updated, Optional.of(existing));
        menuDao.save(updated);
        // 单实体缓存由 CacheEvictionListener 在事务提交后失效；列表缓存需显式清空。
        menuListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.updated(
                EntityCacheNames.MENU, id, null, null, null));
    }

    @AuditLog(action = "menu-delete", description = "删除菜单")
    @Override
    @Transactional
    public void delete(Long id) {
        if (menuDao.existsByParentId(id)) {
            throw new BizException(SystemErrorCode.MENU_HAS_CHILDREN);
        }
        menuDao.deleteRoleMenuByMenuId(id);
        menuDao.removeById(id);
        menuListCache.evictAll();
        eventPublisher.publish(EntityChangedEvent.deleted(
                EntityCacheNames.MENU, id, null, null));
    }

    @Override
    public List<Menu> list() {
        return menuListCache.getOrLoad("__all__", () ->
                menuDao.findAllOrderBySort()
        );
    }

    @Override
    public List<MenuTreeNode> tree() {
        return buildTree(list());
    }

    @Override
    public UserMenuResult myMenus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof io.rosecloud.common.security.model.SecurityUser su) || su.getUserId() == null) {
            return UserMenuResult.empty();
        }
        List<Long> roleIds = menuDao.findRoleIdsByUserId(su.getUserId());
        if (roleIds.isEmpty()) {
            return UserMenuResult.empty();
        }
        List<Menu> menus = findByRoleIds(roleIds);
        List<String> permissions = menus.stream()
                .map(Menu::getPerms)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .toList();
        List<Menu> navigation = menus.stream()
                .filter(m -> m.getType() != null && m.getType() != MenuType.BUTTON.code())
                .filter(m -> m.getVisible() != null && m.getVisible() == 1)
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .toList();
        return new UserMenuResult(buildTree(navigation), permissions);
    }

    private Optional<Menu> findById(Long id) {
        return Optional.ofNullable(menuCache.getOrLoadTransactional(id, () ->
                menuDao.findById(id).orElse(null)
        ));
    }

    /**
     * Resolve menus for a role set via per-role menu-id cache + one batch menu load.
     * Avoids combinatorial {@code menu.list} keys that explode under role mixes.
     */
    private List<Menu> findByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> menuIds = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            menuIds.addAll(roleMenuIdsCache.getOrLoad(roleId, () -> menuDao.findMenuIdsByRoleId(roleId)));
        }
        List<Menu> menus = menuDao.findByIds(menuIds);
        for (Menu menu : menus) {
            menuCache.putIfAbsent(menu.getId(), menu);
        }
        return menus;
    }

    private Menu toMenu(Long id, MenuRequest request) {
        long parentId = request.parentId() == null ? 0L : request.parentId();
        int sort = request.sort() == null ? 0 : request.sort();
        int status = request.status() == null ? 1 : request.status();
        int visible = request.visible() == null ? 1 : request.visible();
        return Menu.of(id, parentId, request.name(), request.type(), request.path(), request.component(),
                request.perms(), request.icon(), sort, status, visible);
    }

    private List<MenuTreeNode> buildTree(List<Menu> menus) {
        Map<Long, List<Menu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return buildChildren(byParent, 0L);
    }

    private List<MenuTreeNode> buildChildren(Map<Long, List<Menu>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(m -> new MenuTreeNode(m, buildChildren(byParent, m.getId())))
                .toList();
    }
}
