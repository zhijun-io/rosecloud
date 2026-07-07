package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    public UserRepositoryImpl(UserMapper userMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                              RoleMenuMapper roleMenuMapper, MenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public Optional<UserAuthInfo> findAuthInfo(String username) {
        UserPO po = userMapper.selectOne(
                new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, username));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(new UserAuthInfo(po.getId(), po.getUsername(), po.getPassword(),
                po.getStatus(), po.getTenantId(), loadRoleCodes(po.getId()), loadPerms(po.getId())));
    }

    private List<String> loadRoleCodes(Long userId) {
        List<UserRolePO> links = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRolePO>().eq(UserRolePO::getUserId, userId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = links.stream().map(UserRolePO::getRoleId).toList();
        List<RolePO> roles = roleMapper.selectList(
                new LambdaQueryWrapper<RolePO>().in(RolePO::getId, roleIds));
        return roles.stream().map(RolePO::getCode).toList();
    }

    /**
     * Aggregates the caller's fine-grained permission codes: the union of
     * non-null {@code sys_menu.perms} reachable through the user's roles. The
     * resulting set is embedded into the JWT at login so endpoint-level
     * {@code @PreAuthorize("hasAuthority('system:user:add')")} rules can be
     * enforced statelessly without re-querying the menu tree on every request.
     */
    private List<String> loadPerms(Long userId) {
        List<Long> roleIds = findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<RoleMenuPO>().in(RoleMenuPO::getRoleId, roleIds))
                .stream().map(RoleMenuPO::getMenuId).toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(
                        new LambdaQueryWrapper<MenuPO>().in(MenuPO::getId, menuIds))
                .stream()
                .map(MenuPO::getPerms)
                .filter(java.util.Objects::nonNull)
                .filter(p -> !p.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.exists(new LambdaQueryWrapper<UserPO>().eq(UserPO::getUsername, username));
    }

    @Override
    public Long insert(User user, String passwordHash) {
        UserPO po = new UserPO();
        po.setUsername(user.username());
        po.setPassword(passwordHash);
        po.setNickname(user.nickname());
        po.setStatus(user.status());
        po.setTenantId(user.tenantId());
        userMapper.insert(po);
        return po.getId();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<User> page(long current, long size, String keyword) {
        Page<UserPO> page = new Page<>(current, size);
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(UserPO::getUsername, keyword).or().like(UserPO::getNickname, keyword);
        }
        wrapper.orderByDesc(UserPO::getCreateTime);
        IPage<UserPO> result = userMapper.selectPage(page, wrapper);
        List<User> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRolePO>().eq(UserRolePO::getUserId, userId))
                .stream().map(UserRolePO::getRoleId).toList();
    }

    @Override
    public List<String> findRoleCodesByUserId(Long userId) {
        List<Long> roleIds = findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RolePO>().in(RolePO::getId, roleIds))
                .stream().map(RolePO::getCode).toList();
    }

    @Override
    public void assignRoles(Long userId, Collection<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRolePO>().eq(UserRolePO::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            UserRolePO po = new UserRolePO();
            po.setUserId(userId);
            po.setRoleId(roleId);
            userRoleMapper.insert(po);
        }
    }

    @Override
    public List<NoticeRecipient> findContacts(Integer targetType, Long tenantId, String roleCode) {
        int type = targetType == null ? NoticeTargetType.GLOBAL.code() : targetType;
        List<UserPO> users;
        if (type == NoticeTargetType.TENANT.code()) {
            users = userMapper.selectList(new LambdaQueryWrapper<UserPO>()
                    .eq(UserPO::getTenantId, tenantId).eq(UserPO::getStatus, 1));
        } else if (type == NoticeTargetType.ROLE.code()) {
            users = findByRole(roleCode);
        } else {
            users = userMapper.selectList(new LambdaQueryWrapper<UserPO>().eq(UserPO::getStatus, 1));
        }
        return users.stream()
                .map(po -> new NoticeRecipient(po.getId(), po.getEmail(), po.getPhone()))
                .toList();
    }

    private List<UserPO> findByRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return List.of();
        }
        RolePO role = roleMapper.selectOne(new LambdaQueryWrapper<RolePO>().eq(RolePO::getCode, roleCode));
        if (role == null) {
            return List.of();
        }
        List<Long> userIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRolePO>().eq(UserRolePO::getRoleId, role.getId()))
                .stream().map(UserRolePO::getUserId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserPO>()
                .in(UserPO::getId, userIds).eq(UserPO::getStatus, 1));
    }

    private User toDomain(UserPO po) {
        return new User(po.getId(), po.getUsername(), po.getNickname(), po.getStatus(), po.getTenantId());
    }
}
