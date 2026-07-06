package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.RoleRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;

    public RoleRepositoryImpl(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public boolean existsByCode(String code) {
        return roleMapper.exists(new LambdaQueryWrapper<RolePO>().eq(RolePO::getCode, code));
    }

    @Override
    public Long insert(Role role) {
        RolePO po = new RolePO();
        po.setCode(role.code());
        po.setName(role.name());
        roleMapper.insert(po);
        return po.getId();
    }

    @Override
    public PageResult<Role> page(long current, long size, String keyword) {
        Page<RolePO> page = new Page<>(current, size);
        LambdaQueryWrapper<RolePO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(RolePO::getCode, keyword).or().like(RolePO::getName, keyword);
        }
        wrapper.orderByDesc(RolePO::getCreateTime);
        IPage<RolePO> result = roleMapper.selectPage(page, wrapper);
        List<Role> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public boolean existsById(Long id) {
        return roleMapper.selectById(id) != null;
    }

    @Override
    public List<Long> findMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<RoleMenuPO>().eq(RoleMenuPO::getRoleId, roleId))
                .stream().map(RoleMenuPO::getMenuId).toList();
    }

    @Override
    public void assignMenus(Long roleId, Collection<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuPO>().eq(RoleMenuPO::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds) {
            RoleMenuPO po = new RoleMenuPO();
            po.setRoleId(roleId);
            po.setMenuId(menuId);
            roleMenuMapper.insert(po);
        }
    }

    private Role toDomain(RolePO po) {
        return new Role(po.getId(), po.getCode(), po.getName());
    }
}
