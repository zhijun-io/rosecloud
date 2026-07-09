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
import java.util.Optional;
import java.time.LocalDateTime;

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
        return roleMapper.exists(new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code));
    }

    @Override
    public Long insert(Role role) {
        RoleEntity po = new RoleEntity();
        po.setCode(role.getCode());
        po.setName(role.getName());
        roleMapper.insert(po);
        return po.getId();
    }

    @Override
    public PageResult<Role> page(long current, long size, String keyword) {
        Page<RoleEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(RoleEntity::getCode, keyword).or().like(RoleEntity::getName, keyword);
        }
        wrapper.orderByDesc(RoleEntity::getCreateTime);
        IPage<RoleEntity> result = roleMapper.selectPage(page, wrapper);
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
                        new LambdaQueryWrapper<RoleMenuEntity>().eq(RoleMenuEntity::getRoleId, roleId))
                .stream().map(RoleMenuEntity::getMenuId).toList();
    }

    @Override
    public void assignMenus(Long roleId, Collection<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuEntity>().eq(RoleMenuEntity::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds) {
            RoleMenuEntity po = new RoleMenuEntity();
            po.setRoleId(roleId);
            po.setMenuId(menuId);
            roleMenuMapper.insert(po);
        }
    }

    @Override
    public Optional<Role> findById(Long id) {
        return Optional.ofNullable(roleMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return Optional.ofNullable(roleMapper.selectOne(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code))).map(this::toDomain);
    }

    private Role toDomain(RoleEntity po) {
        return new Role(po.getId(), po.getCode(), po.getName(), po.getCreateTime(), po.getCreateBy(),
                po.getUpdateTime(), po.getUpdateBy());
    }
}
