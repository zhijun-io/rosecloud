package io.rosecloud.system.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import io.rosecloud.api.user.UserActivationInfo;
import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserCredentialMapper userCredentialMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final ObjectMapper objectMapper;

    public UserRepositoryImpl(UserMapper userMapper, UserCredentialMapper userCredentialMapper,
                              UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                              RoleMenuMapper roleMenuMapper, MenuMapper menuMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.userCredentialMapper = userCredentialMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SecurityUser> loadByUsername(String username) {
        UserEntity po = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                        .or()
                        .eq(UserEntity::getPhone, username)));
        if (po == null) {
            return Optional.empty();
        }
        UserCredentialEntity credential = credentialByUserId(po.getId());
        String password = credential == null ? null : credential.getPassword();

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : loadRoleCodes(po.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String perm : loadPerms(po.getId())) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }

        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, loginName(po));
        return Optional.of(new SecurityUser(
                po.getId(), loginName(po), loginName(po), password,
                po.getStatus() != null && po.getStatus() == 1,
                principal, authorities));
    }

    @Override
    public Optional<UserActivationInfo> findActivationByToken(String activateToken) {
        if (activateToken == null || activateToken.isBlank()) {
            return Optional.empty();
        }
        UserCredentialEntity credential = userCredentialMapper.selectOne(
                new LambdaQueryWrapper<UserCredentialEntity>().eq(UserCredentialEntity::getActivateToken, activateToken));
        if (credential == null) {
            return Optional.empty();
        }
        UserEntity user = userMapper.selectById(credential.getUserId());
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(toActivationInfo(user, credential));
    }

    @Override
    public Optional<UserActivationInfo> findActivationByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                        .or()
                        .eq(UserEntity::getPhone, username)));
        if (user == null) {
            return Optional.empty();
        }
        UserCredentialEntity credential = credentialByUserId(user.getId());
        return Optional.of(toActivationInfo(user, credential));
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
        return userMapper.exists(new LambdaQueryWrapper<UserEntity>()
                .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                        .or()
                        .eq(UserEntity::getPhone, username)));
    }

    @Override
    public Long insert(User user, String passwordHash) {
        UserEntity po = new UserEntity();
        po.setNickname(user.getNickname());
        po.setStatus(user.getStatus());
        po.setTenantId(user.getTenantId());
        if (isEmail(user.getUsername())) {
            po.setEmail(user.getUsername());
        } else if (isPhone(user.getUsername())) {
            po.setPhone(user.getUsername());
        }
        po.setAdditionalInfo(writeJson(user.getAdditionalInfo()));
        userMapper.insert(po);
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(po.getId());
        credential.setPassword(passwordHash);
        credential.setPasswordChangedTime(passwordHash == null ? null : LocalDateTime.now());
        userCredentialMapper.insert(credential);
        return po.getId();
    }

    @Override
    public void saveActivationToken(Long userId, String activateToken, LocalDateTime expireTime,
                                    LocalDateTime sendTime, Long version) {
        UserCredentialEntity credential = credentialByUserId(userId);
        if (credential == null) {
            throw new IllegalStateException("Missing user credential for userId=" + userId);
        }
        credential.setActivateToken(activateToken);
        credential.setExpireTime(expireTime);
        credential.setUsedTime(null);
        credential.setSendTime(sendTime);
        credential.setVersion(version);
        userCredentialMapper.updateById(credential);
    }

    @Override
    public void confirmActivation(Long userId, String encodedPassword, LocalDateTime usedTime) {
        UserCredentialEntity credential = credentialByUserId(userId);
        if (credential == null) {
            throw new IllegalStateException("Missing user credential for userId=" + userId);
        }
        credential.setPassword(encodedPassword);
        credential.setPasswordChangedTime(usedTime);
        credential.setActivateToken(null);
        credential.setExpireTime(null);
        credential.setUsedTime(usedTime);
        userCredentialMapper.updateById(credential);

        UserEntity user = userMapper.selectById(userId);
        if (user != null) {
            user.setStatus(1);
            userMapper.updateById(user);
        }
    }

    @Override
    public void updateLastLoginTime(Long userId, LocalDateTime lastLoginTime) {
        UserCredentialEntity credential = credentialByUserId(userId);
        if (credential == null) {
            throw new IllegalStateException("Missing user credential for userId=" + userId);
        }
        credential.setLastLoginTime(lastLoginTime);
        userCredentialMapper.updateById(credential);
    }

    @Override
    public void updatePassword(Long userId, String passwordHash, LocalDateTime passwordChangedTime) {
        UserCredentialEntity credential = credentialByUserId(userId);
        if (credential == null) {
            throw new IllegalStateException("Missing user credential for userId=" + userId);
        }
        credential.setPassword(passwordHash);
        credential.setPasswordChangedTime(passwordChangedTime);
        userCredentialMapper.updateById(credential);
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
            wrapper.nested(w -> w.like(UserEntity::getEmail, keyword)
                    .or()
                    .like(UserEntity::getPhone, keyword)
                    .or()
                    .like(UserEntity::getNickname, keyword));
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
    public List<NoticeRecipient> findContacts(Integer targetType, String tenantId, String roleCode, String username) {
        int type = targetType == null ? NoticeTargetType.GLOBAL.code() : targetType;
        List<UserEntity> users;
        if (type == NoticeTargetType.TENANT.code()) {
            users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                    .eq(UserEntity::getTenantId, tenantId).eq(UserEntity::getStatus, 1));
        } else if (type == NoticeTargetType.ROLE.code()) {
            users = findByRole(roleCode);
        } else if (type == NoticeTargetType.USER.code()) {
            users = findByUsername(username);
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

    private List<UserEntity> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                        .or()
                        .eq(UserEntity::getPhone, username)));
        return user == null ? List.of() : List.of(user);
    }

    private User toDomain(UserEntity po) {
        return new User(po.getId(), loginName(po), po.getNickname(), po.getStatus(), po.getTenantId(),
                readJson(po.getAdditionalInfo()));
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

    private UserCredentialEntity credentialByUserId(Long userId) {
        return userCredentialMapper.selectOne(
                new LambdaQueryWrapper<UserCredentialEntity>().eq(UserCredentialEntity::getUserId, userId));
    }

    private boolean isEmail(String username) {
        return username != null && username.contains("@");
    }

    private boolean isPhone(String username) {
        return username != null && username.matches("\\d{6,}");
    }

    private UserActivationInfo toActivationInfo(UserEntity user, UserCredentialEntity credential) {
        return new UserActivationInfo(user.getId(), loginName(user), user.getTenantId(),
                credential == null ? null : credential.getActivateToken(),
                credential == null ? null : credential.getExpireTime(),
                credential == null ? null : credential.getUsedTime(),
                credential == null ? null : credential.getSendTime(),
                credential == null ? null : credential.getVersion());
    }

    private String loginName(UserEntity user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        return null;
    }
}
