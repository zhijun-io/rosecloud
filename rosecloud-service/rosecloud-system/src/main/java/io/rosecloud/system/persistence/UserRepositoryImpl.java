package io.rosecloud.system.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public UserRepositoryImpl(UserMapper userMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                              RoleMenuMapper roleMenuMapper, MenuMapper menuMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<UserAuthInfo> findAuthInfo(String username) {
        UserEntity po = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(new UserAuthInfo(po.getId(), po.getUsername(), po.getPassword(),
                po.getStatus(), po.getTenantId(), loadRoleCodes(po.getId()), loadPerms(po.getId())));
    }

    private List<String> loadRoleCodes(Long userId) {
        List<UserRoleEntity> links = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = links.stream().map(UserRoleEntity::getRoleId).toList();
        List<RoleEntity> roles = roleMapper.selectList(
                new LambdaQueryWrapper<RoleEntity>().in(RoleEntity::getId, roleIds));
        return roles.stream().map(RoleEntity::getCode).toList();
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
                        new LambdaQueryWrapper<RoleMenuEntity>().in(RoleMenuEntity::getRoleId, roleIds))
                .stream().map(RoleMenuEntity::getMenuId).toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(
                        new LambdaQueryWrapper<MenuEntity>().in(MenuEntity::getId, menuIds))
                .stream()
                .map(MenuEntity::getPerms)
                .filter(java.util.Objects::nonNull)
                .filter(p -> !p.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.exists(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
    }

    @Override
    public Long insert(User user, String passwordHash) {
        UserEntity po = new UserEntity();
        po.setUsername(user.getUsername());
        po.setPassword(passwordHash);
        po.setNickname(user.getNickname());
        po.setStatus(user.getStatus());
        po.setTenantId(user.getTenantId());
        po.setExtra(writeJson(user.getAdditionalInfo()));
        userMapper.insert(po);
        return po.getId();
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<User> page(long current, long size, String keyword) {
        Page<UserEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(UserEntity::getUsername, keyword).or().like(UserEntity::getNickname, keyword);
        }
        wrapper.orderByDesc(UserEntity::getCreateTime);
        IPage<UserEntity> result = userMapper.selectPage(page, wrapper);
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
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId))
                .stream().map(UserRoleEntity::getRoleId).toList();
    }

    @Override
    public List<String> findRoleCodesByUserId(Long userId) {
        List<Long> roleIds = findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>().in(RoleEntity::getId, roleIds))
                .stream().map(RoleEntity::getCode).toList();
    }

    @Override
    public void assignRoles(Long userId, Collection<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            UserRoleEntity po = new UserRoleEntity();
            po.setUserId(userId);
            po.setRoleId(roleId);
            userRoleMapper.insert(po);
        }
    }

    @Override
    public List<NoticeRecipient> findContacts(Integer targetType, Long tenantId, String roleCode) {
        int type = targetType == null ? NoticeTargetType.GLOBAL.code() : targetType;
        List<UserEntity> users;
        if (type == NoticeTargetType.TENANT.code()) {
            users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                    .eq(UserEntity::getTenantId, tenantId).eq(UserEntity::getStatus, 1));
        } else if (type == NoticeTargetType.ROLE.code()) {
            users = findByRole(roleCode);
        } else {
            users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getStatus, 1));
        }
        return users.stream()
                .map(po -> new NoticeRecipient(po.getId(), po.getEmail(), po.getPhone()))
                .toList();
    }

    private List<UserEntity> findByRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return List.of();
        }
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, roleCode));
        if (role == null) {
            return List.of();
        }
        List<Long> userIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getRoleId, role.getId()))
                .stream().map(UserRoleEntity::getUserId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .in(UserEntity::getId, userIds).eq(UserEntity::getStatus, 1));
    }

    private User toDomain(UserEntity po) {
        return new User(po.getId(), po.getUsername(), po.getNickname(), po.getStatus(), po.getTenantId(),
                readJson(po.getExtra()));
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid user extra JSON", ex);
        }
    }

    private String writeJson(JsonNode value) {
        return value == null || value.isNull() ? null : value.toString();
    }
}
