package io.rosecloud.system.persistence;

import io.rosecloud.starter.data.dao.MyBatisDao;
import java.util.Collection;
import java.util.List;
import io.rosecloud.system.domain.Menu;
import io.rosecloud.system.persistence.RoleMenuEntity;
import io.rosecloud.system.persistence.RoleMenuMapper;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.persistence.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

@Repository
public class MenuDao extends MyBatisDao<Menu, Long, MenuEntity> {

    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    public MenuDao(MenuMapper menuMapper, RoleMenuMapper roleMenuMapper, UserRoleMapper userRoleMapper) {
        super(menuMapper, MenuEntity.class);
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    // ==== 父菜单检查 ====

    public boolean existsByParentId(Long parentId) {
        return mapper.exists(new LambdaQueryWrapper<MenuEntity>().eq(MenuEntity::getParentId, parentId));
    }

    // ==== 全量查询 ====

    public List<Menu> findAllOrderBySort() {
        return mapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                        .orderByAsc(MenuEntity::getSort))
                .stream().map(MenuEntity::toData).toList();
    }

    // ==== Role-Menu 关联 ====

    public void deleteRoleMenuByMenuId(Long menuId) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuEntity>()
                .eq(RoleMenuEntity::getMenuId, menuId));
    }

    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<RoleMenuEntity>().eq(RoleMenuEntity::getRoleId, roleId))
                .stream()
                .map(RoleMenuEntity::getMenuId)
                .toList();
    }

    public List<Menu> findMenusByRoleIds(Collection<Long> roleIds) {
        List<RoleMenuEntity> links = roleMenuMapper.selectList(
                new LambdaQueryWrapper<RoleMenuEntity>().in(RoleMenuEntity::getRoleId, roleIds));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = links.stream().map(RoleMenuEntity::getMenuId).distinct().toList();
        return findByIds(menuIds);
    }

    public List<Menu> findByIds(Collection<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                        .in(MenuEntity::getId, menuIds)
                        .orderByAsc(MenuEntity::getSort)
                        .orderByAsc(MenuEntity::getId))
                .stream().map(MenuEntity::toData).toList();
    }

    // ==== User-Role 关联 ====

    public List<Long> findRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId))
                .stream()
                .map(UserRoleEntity::getRoleId)
                .toList();
    }

    @Override
    protected Long getId(Menu domain) {
        return domain.getId();
    }

    @Override
    protected MenuEntity toEntity(Menu domain) {
        return new MenuEntity().toEntity(domain);
    }
}
