package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.domain.MenuRepository;
import io.rosecloud.system.domain.MenuType;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.MenuService;
import io.rosecloud.system.service.dto.MenuRequest;
import io.rosecloud.system.service.dto.MenuTreeNode;
import io.rosecloud.system.service.dto.UserMenuResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;

    public MenuServiceImpl(MenuRepository menuRepository, UserRepository userRepository) {
        this.menuRepository = menuRepository;
        this.userRepository = userRepository;
    }

    @AuditLog(action = "menu-create", description = "创建菜单")
    @Override
    public Long create(MenuRequest request) {
        return menuRepository.insert(toMenu(null, request));
    }

    @AuditLog(action = "menu-update", description = "修改菜单")
    @Override
    public void update(Long id, MenuRequest request) {
        menuRepository.findById(id).orElseThrow(() -> new BizException(SystemErrorCode.MENU_NOT_FOUND));
        menuRepository.update(toMenu(id, request));
    }

    @AuditLog(action = "menu-delete", description = "删除菜单")
    @Override
    public void delete(Long id) {
        if (menuRepository.existsByParentId(id)) {
            throw new BizException(SystemErrorCode.MENU_HAS_CHILDREN);
        }
        menuRepository.deleteById(id);
    }

    @Override
    public List<Menu> list() {
        return menuRepository.findAll();
    }

    @Override
    public List<MenuTreeNode> tree() {
        return buildTree(menuRepository.findAll());
    }

    @Override
    public UserMenuResult myMenus() {
        CurrentUser current = UserContext.get();
        if (current == null || current.userId() == null) {
            return UserMenuResult.empty();
        }
        List<Long> roleIds = userRepository.findRoleIdsByUserId(current.userId());
        if (roleIds.isEmpty()) {
            return UserMenuResult.empty();
        }
        List<Menu> menus = menuRepository.findByRoleIds(roleIds);
        List<String> permissions = menus.stream()
                .map(Menu::perms)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .toList();
        List<Menu> navigation = menus.stream()
                .filter(m -> m.type() != null && m.type() != MenuType.BUTTON.code())
                .filter(m -> m.visible() != null && m.visible() == 1)
                .filter(m -> m.status() != null && m.status() == 1)
                .toList();
        return new UserMenuResult(buildTree(navigation), permissions);
    }

    private Menu toMenu(Long id, MenuRequest request) {
        long parentId = request.parentId() == null ? 0L : request.parentId();
        int sort = request.sort() == null ? 0 : request.sort();
        int status = request.status() == null ? 1 : request.status();
        int visible = request.visible() == null ? 1 : request.visible();
        return new Menu(id, parentId, request.name(), request.type(), request.path(), request.component(),
                request.perms(), request.icon(), sort, status, visible);
    }

    private List<MenuTreeNode> buildTree(List<Menu> menus) {
        Map<Long, List<Menu>> byParent = menus.stream()
                .collect(Collectors.groupingBy(m -> m.parentId() == null ? 0L : m.parentId()));
        return buildChildren(byParent, 0L);
    }

    private List<MenuTreeNode> buildChildren(Map<Long, List<Menu>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(m -> new MenuTreeNode(m, buildChildren(byParent, m.id())))
                .toList();
    }
}
