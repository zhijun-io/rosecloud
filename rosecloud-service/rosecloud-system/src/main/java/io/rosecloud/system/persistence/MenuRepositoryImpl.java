package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.domain.MenuRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    public MenuRepositoryImpl(MenuMapper menuMapper, RoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public Optional<Menu> findById(Long id) {
        return Optional.ofNullable(menuMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        return menuMapper.exists(new LambdaQueryWrapper<MenuPO>().eq(MenuPO::getParentId, parentId));
    }

    @Override
    public Long insert(Menu menu) {
        MenuPO po = toPO(menu);
        po.setId(null);
        menuMapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(Menu menu) {
        menuMapper.updateById(toPO(menu));
    }

    @Override
    public void deleteById(Long id) {
        menuMapper.deleteById(id);
    }

    @Override
    public List<Menu> findAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<MenuPO>()
                        .orderByAsc(MenuPO::getSort))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Menu> findByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<RoleMenuPO> links = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenuPO>().in(RoleMenuPO::getRoleId, roleIds));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = links.stream().map(RoleMenuPO::getMenuId).distinct().toList();
        return menuMapper.selectList(new LambdaQueryWrapper<MenuPO>()
                        .in(MenuPO::getId, menuIds)
                        .orderByAsc(MenuPO::getSort))
                .stream().map(this::toDomain).toList();
    }

    private Menu toDomain(MenuPO po) {
        return new Menu(po.getId(), po.getParentId(), po.getName(), po.getType(), po.getPath(),
                po.getComponent(), po.getPerms(), po.getIcon(), po.getSort(), po.getStatus(), po.getVisible());
    }

    private MenuPO toPO(Menu m) {
        MenuPO po = new MenuPO();
        po.setId(m.id());
        po.setParentId(m.parentId());
        po.setName(m.name());
        po.setType(m.type());
        po.setPath(m.path());
        po.setComponent(m.component());
        po.setPerms(m.perms());
        po.setIcon(m.icon());
        po.setSort(m.sort());
        po.setStatus(m.status());
        po.setVisible(m.visible());
        return po;
    }
}
