package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.rosecloud.starter.data.dao.MyBatisDao;
import io.rosecloud.system.domain.Role;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class RoleDao extends MyBatisDao<Role, Long, RoleEntity> {

    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleDao(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper, UserRoleMapper userRoleMapper) {
        super(roleMapper, RoleEntity.class);
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    // ==== Role-Menu 关联 ====

    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RoleMenuEntity>()
                        .eq(RoleMenuEntity::getRoleId, roleId))
                .stream()
                .map(RoleMenuEntity::getMenuId)
                .toList();
    }

    public void deleteRoleMenuByRoleId(Long roleId) {
        roleMenuMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RoleMenuEntity>()
                        .eq(RoleMenuEntity::getRoleId, roleId));
    }

    public void assignMenuToRole(Long roleId, Long menuId) {
        assignMenusToRole(roleId, List.of(menuId));
    }

    public void assignMenusToRole(Long roleId, Collection<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<RoleMenuEntity> rows = menuIds.stream().map(menuId -> {
            RoleMenuEntity po = new RoleMenuEntity();
            po.setId(IdWorker.getId());
            po.setRoleId(roleId);
            po.setMenuId(menuId);
            return po;
        }).toList();
        roleMenuMapper.insertBatch(rows);
    }

    // ==== User-Role 关联 ====

    public List<Long> findUserIdsByRoleId(Long roleId) {
        return userRoleMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRoleEntity>()
                                .eq(UserRoleEntity::getRoleId, roleId))
                .stream()
                .map(UserRoleEntity::getUserId)
                .toList();
    }

    @Override
    protected Long getId(Role domain) {
        return domain.getId();
    }

    @Override
    protected RoleEntity toEntity(Role domain) {
        RoleEntity entity = new RoleEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setCreateTime(domain.getCreateTime());
        entity.setCreateBy(domain.getCreateBy());
        entity.setUpdateTime(domain.getUpdateTime());
        entity.setUpdateBy(domain.getUpdateBy());
        return entity;
    }
}
