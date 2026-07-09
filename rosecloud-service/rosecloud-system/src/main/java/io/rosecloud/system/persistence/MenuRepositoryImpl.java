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
        return menuMapper.exists(new LambdaQueryWrapper<MenuEntity>().eq(MenuEntity::getParentId, parentId));
    }

    @Override
    public Long insert(Menu menu) {
        MenuEntity po = toEntity(menu);
        po.setId(null);
        menuMapper.insert(po);
        return po.getId();
    }

    @Override
    public void update(Menu menu) {
        menuMapper.updateById(toEntity(menu));
    }

    @Override
    public void deleteById(Long id) {
        menuMapper.deleteById(id);
    }

    @Override
    public List<Menu> findAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                        .orderByAsc(MenuEntity::getSort))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Menu> findByRoleIds(Collection<Long> roleIds) {
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
                .stream().map(this::toDomain).toList();
    }

    private Menu toDomain(MenuEntity po) {
        return new Menu(po.getId(), po.getParentId(), po.getName(), po.getType(), po.getPath(),
                po.getComponent(), po.getPerms(), po.getIcon(), po.getSort(), po.getStatus(), po.getVisible(),
                po.getCreateTime(), po.getCreateBy(), po.getUpdateTime(), po.getUpdateBy());
    }

    private MenuEntity toEntity(Menu m) {
        MenuEntity po = new MenuEntity();
        po.setId(m.getId());
        po.setParentId(m.getParentId());
        po.setName(m.getName());
        po.setType(m.getType());
        po.setPath(m.getPath());
        po.setComponent(m.getComponent());
        po.setPerms(m.getPerms());
        po.setIcon(m.getIcon());
        po.setSort(m.getSort());
        po.setStatus(m.getStatus());
        po.setVisible(m.getVisible());
        po.setCreateTime(m.getCreateTime());
        po.setCreateBy(m.getCreateBy());
        po.setUpdateTime(m.getUpdateTime());
        po.setUpdateBy(m.getUpdateBy());
        return po;
    }
}
