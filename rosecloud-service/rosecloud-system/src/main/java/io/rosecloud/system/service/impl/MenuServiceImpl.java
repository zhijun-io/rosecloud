package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.domain.MenuType;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.MenuEntity;
import io.rosecloud.system.persistence.MenuMapper;
import io.rosecloud.system.persistence.RoleMenuEntity;
import io.rosecloud.system.persistence.RoleMenuMapper;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.persistence.UserRoleMapper;
import io.rosecloud.system.service.MenuService;
import io.rosecloud.system.service.dto.MenuRequest;
import io.rosecloud.system.service.dto.MenuTreeNode;
import io.rosecloud.system.service.dto.UserMenuResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;
    @AuditLog(action = "menu-create", description = "创建菜单")
    @Override
    public Long create(MenuRequest request) {
        MenuEntity po = new MenuEntity().toEntity(toMenu(null, request));
        po.setId(null);
        menuMapper.insert(po);
        return po.getId();
    }

    @AuditLog(action = "menu-update", description = "修改菜单")
    @Override
    public void update(Long id, MenuRequest request) {
        findById(id).orElseThrow(() -> new BizException(SystemErrorCode.MENU_NOT_FOUND));
        menuMapper.updateById(new MenuEntity().toEntity(toMenu(id, request)));
    }

    @AuditLog(action = "menu-delete", description = "删除菜单")
    @Override
    @Transactional
    public void delete(Long id) {
        if (menuMapper.exists(new LambdaQueryWrapper<MenuEntity>().eq(MenuEntity::getParentId, id))) {
            throw new BizException(SystemErrorCode.MENU_HAS_CHILDREN);
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuEntity>().eq(RoleMenuEntity::getMenuId, id));
        menuMapper.deleteById(id);
    }

    @Override
    public List<Menu> list() {
        return menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                        .orderByAsc(MenuEntity::getSort))
                .stream().map(MenuEntity::toData).toList();
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
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, su.getUserId()))
                .stream().map(UserRoleEntity::getRoleId).toList();
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
        return Optional.ofNullable(menuMapper.selectById(id)).map(MenuEntity::toData);
    }

    private List<Menu> findByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<RoleMenuEntity> links = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenuEntity>().in(RoleMenuEntity::getRoleId, roleIds));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = links.stream().map(RoleMenuEntity::getMenuId).distinct().toList();
        return menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                        .in(MenuEntity::getId, menuIds)
                        .orderByAsc(MenuEntity::getSort))
                .stream().map(MenuEntity::toData).toList();
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
