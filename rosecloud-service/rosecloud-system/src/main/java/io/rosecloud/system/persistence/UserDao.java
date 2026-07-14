package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.data.dao.MyBatisDao;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.error.SystemErrorCode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserDao extends MyBatisDao<User, Long, UserEntity> {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    public UserDao(UserMapper userMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                   RoleMenuMapper roleMenuMapper, MenuMapper menuMapper) {
        super(userMapper, UserEntity.class);
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    public boolean existsByUsername(String username) {
        return mapper.exists(new LambdaQueryWrapper<UserEntity>()
                .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                        .or()
                        .eq(UserEntity::getPhone, username)));
    }

    public Optional<User> findByEmailOrPhone(String username) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                        .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                                .or()
                                .eq(UserEntity::getPhone, username))))
                .map(UserEntity::toData);
    }

    public User insert(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BizException(SystemErrorCode.USERNAME_INVALID);
        }
        if (!isEmail(user.getUsername()) && !isPhone(user.getUsername())) {
            throw new BizException(SystemErrorCode.USERNAME_INVALID);
        }
        UserEntity po = toEntity(user);
        if (isEmail(user.getUsername())) {
            po.setEmail(user.getUsername());
        } else if (isPhone(user.getUsername())) {
            po.setPhone(user.getUsername());
        }
        po.setAdditionalInfo(writeJson(user.getAdditionalInfo()));
        mapper.insert(po);
        return po.toData();
    }

    /** Delete all role assignments for a user. */
    public void deleteUserRoles(Long userId) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId));
    }

    /** Insert a single role assignment for a user. */
    public void insertUserRole(Long userId, Long roleId) {
        UserRoleEntity po = new UserRoleEntity();
        po.setUserId(userId);
        po.setRoleId(roleId);
        userRoleMapper.insert(po);
    }

    /** Find role IDs assigned to a user. */
    public List<Long> findRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId))
                .stream().map(UserRoleEntity::getRoleId).toList();
    }

    public List<String> loadRoleCodes(Long userId) {
        List<Long> roleIds = findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>().in(RoleEntity::getId, roleIds))
                .stream().map(RoleEntity::getCode).toList();
    }

    public List<String> findRoleCodesByUserId(Long userId) {
        List<Long> roleIds = findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>().in(RoleEntity::getId, roleIds))
                .stream().map(RoleEntity::getCode).toList();
    }

    public List<String> loadPerms(Long userId) {
        List<Long> roleIds = findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<RoleMenuEntity>().in(RoleMenuEntity::getRoleId, roleIds))
                .stream().map(RoleMenuEntity::getMenuId).toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(
                        new LambdaQueryWrapper<MenuEntity>().in(MenuEntity::getId, menuIds))
                .stream()
                .map(MenuEntity::getPerms)
                .filter(Objects::nonNull)
                .filter(p -> !p.isBlank())
                .distinct()
                .toList();
    }

    private static boolean isEmail(String username) {
        return username != null && username.contains("@");
    }

    private static boolean isPhone(String username) {
        return username != null && username.matches("\\d{6,}");
    }

    private static String writeJson(com.fasterxml.jackson.databind.JsonNode value) {
        return value == null || value.isNull() ? null : value.toString();
    }

    @Override
    protected Long getId(User domain) {
        return domain.getId();
    }

    @Override
    protected UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        entity.setNickname(domain.getNickname());
        entity.setStatus(domain.getStatus());
        entity.setTenantId(domain.getTenantId());
        return entity;
    }
}
