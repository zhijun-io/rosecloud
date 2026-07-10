package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.api.user.UserApi;
import io.rosecloud.api.user.UserPasswordUpdateRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.MenuEntity;
import io.rosecloud.system.persistence.MenuMapper;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.persistence.RoleMapper;
import io.rosecloud.system.persistence.RoleMenuEntity;
import io.rosecloud.system.persistence.RoleMenuMapper;
import io.rosecloud.system.persistence.UserCredentialEntity;
import io.rosecloud.system.persistence.UserCredentialMapper;
import io.rosecloud.system.persistence.UserEntity;
import io.rosecloud.system.persistence.UserMapper;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.persistence.UserRoleMapper;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;
import io.rosecloud.system.support.PasswordPolicyValidator;
import io.rosecloud.system.support.TenantIdSupport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService, UserApi {

    private final UserMapper userMapper;
    private final UserCredentialMapper userCredentialMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessionStore;

    public UserServiceImpl(UserMapper userMapper, UserCredentialMapper userCredentialMapper,
                          UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                          RoleMenuMapper roleMenuMapper, MenuMapper menuMapper, ObjectMapper objectMapper,
                          PasswordEncoder passwordEncoder, SessionStore sessionStore) {
        this.userMapper = userMapper;
        this.userCredentialMapper = userCredentialMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.sessionStore = sessionStore;
    }

    // ==================== helper ====================

    private static SecurityUser currentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new BizException(SecurityErrorCode.UNAUTHORIZED);
        }
        return securityUser;
    }

    // ==================== crud ====================

    @AuditLog(action = "user-create", description = "创建用户")
    @Transactional
    @Override
    public Long create(UserCreateRequest request) {
        SecurityUser securityUser = currentSecurityUser();
        String targetTenant = resolveCreateTenant(securityUser, request.tenantId());
        if (existsByUsername(request.username())) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        PasswordPolicyValidator.validate(request.password());
        User user = new User(null, request.username(), request.nickname(), 1, targetTenant, null);
        return insert(user, passwordEncoder.encode(request.password()));
    }

    /**
     * Confines user creation to the caller's own tenant. The {@code TenantLineInnerInterceptor}
     * does not scope INSERTs, so a tenant admin supplying an arbitrary {@code tenantId} in the
     * request body would otherwise be able to provision users under any tenant. Only the
     * platform admin (system tenant) may place a user under an explicit tenant.
     */
    private static String resolveCreateTenant(SecurityUser securityUser, String requestedTenantId) {
        if (TenantContextHolder.SYSTEM_TENANT_ID.equals(securityUser.getTenantId())) {
            return TenantIdSupport.requireValid(requestedTenantId);
        }
        return TenantIdSupport.normalize(securityUser.getTenantId());
    }

    @Override
    @Transactional
    public Long createWithHash(String username, String passwordHash, String nickname, String tenantId) {
        if (existsByUsername(username)) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, username, nickname, 1, TenantIdSupport.requireValid(tenantId), null);
        return insert(user, passwordHash);
    }

    @Override
    @Transactional
    public Long createWithoutPassword(String username, String nickname, String tenantId) {
        if (existsByUsername(username)) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, username, nickname, 0, TenantIdSupport.requireValid(tenantId), null);
        return insert(user, null);
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
    public User get(Long id) {
        return findById(id).orElse(null);
    }

    @AuditLog(action = "user-delete", description = "删除用户")
    @Override
    @Transactional
    public void delete(Long id) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, id));
        userMapper.deleteById(id);
    }

    @Override
    public SecurityUser loadByUsername(String username) {
        return loadByUsernameInternal(username)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        SecurityUser securityUser = currentSecurityUser();
        SecurityUser authInfo = loadByUsernameInternal(securityUser.getUsername())
                .orElseThrow(() -> new BizException(SecurityErrorCode.UNAUTHORIZED));
        if (authInfo.getPassword() == null || authInfo.getPassword().isBlank()
                || !passwordEncoder.matches(request.currentPassword(), authInfo.getPassword())) {
            throw new BizException(SecurityErrorCode.BAD_CREDENTIALS);
        }
        PasswordPolicyValidator.validateChange(request.currentPassword(), request.newPassword());
        updatePassword(securityUser.getUserId(),
                passwordEncoder.encode(request.newPassword()), LocalDateTime.now());
        // The password is embedded in the JWT; revoke existing sessions so a previously
        // valid token (e.g. one held by an attacker before the change) cannot be reused.
        sessionStore.revokeByUserId(securityUser.getUserId());
    }

    @AuditLog(action = "user-assign-roles", description = "用户角色授权")
    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (findById(userId).isEmpty()) {
            throw new BizException(SystemErrorCode.USER_NOT_FOUND);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRoleEntity po = new UserRoleEntity();
                po.setUserId(userId);
                po.setRoleId(roleId);
                userRoleMapper.insert(po);
            }
        }
        // Roles/permissions are cached in the JWT; revoke the user's sessions so the next
        // request is forced to re-authenticate with the updated authority set.
        sessionStore.revokeByUserId(userId);
    }

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId))
                .stream().map(UserRoleEntity::getRoleId).toList();
    }

    @Override
    public UserProfile me() {
        SecurityUser securityUser = currentSecurityUser();
        User user = findById(securityUser.getUserId())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        return new UserProfile(user, findRoleCodesByUserId(securityUser.getUserId()));
    }

    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequest request) {
        SecurityUser securityUser = currentSecurityUser();
        if (!securityUser.getUserId().equals(userId)) {
            throw new BizException(SecurityErrorCode.UNAUTHORIZED);
        }
        SecurityUser authInfo = loadByUsernameInternal(securityUser.getUsername())
                .orElseThrow(() -> new BizException(SecurityErrorCode.UNAUTHORIZED));
        if (authInfo.getPassword() == null || authInfo.getPassword().isBlank()
                || !passwordEncoder.matches(request.currentPassword(), authInfo.getPassword())) {
            throw new BizException(SecurityErrorCode.BAD_CREDENTIALS);
        }
        PasswordPolicyValidator.validateChange(request.currentPassword(), request.newPassword());
        updatePassword(userId, passwordEncoder.encode(request.newPassword()), LocalDateTime.now());
        sessionStore.revokeByUserId(userId);
    }

    @Override
    public SecurityUser loadUserByUsername(String username) {
        return loadByUsernameInternal(username).orElse(null);
    }

    // ==================== repository-inlined logic ====================

    private boolean existsByUsername(String username) {
        return userMapper.exists(new LambdaQueryWrapper<UserEntity>()
                .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                        .or()
                        .eq(UserEntity::getPhone, username)));
    }

    private Long insert(User user, String passwordHash) {
        if (user.getUsername() == null || user.getUsername().isBlank()
                || (!isEmail(user.getUsername()) && !isPhone(user.getUsername()))) {
            throw new BizException(SystemErrorCode.USERNAME_INVALID);
        }
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

    private void updatePassword(Long userId, String passwordHash, LocalDateTime passwordChangedTime) {
        UserCredentialEntity credential = credentialByUserId(userId);
        if (credential == null) {
            throw new IllegalStateException("Missing user credential for userId=" + userId);
        }
        credential.setPassword(passwordHash);
        credential.setPasswordChangedTime(passwordChangedTime);
        userCredentialMapper.updateById(credential);
    }

    private Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
    }

    private Optional<SecurityUser> loadByUsernameInternal(String username) {
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
                po.getTenantId(), principal, authorities));
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

    private List<String> findRoleCodesByUserId(Long userId) {
        List<Long> roleIds = findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>().in(RoleEntity::getId, roleIds))
                .stream().map(RoleEntity::getCode).toList();
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

    private String loginName(UserEntity user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        return null;
    }

    private User toDomain(UserEntity po) {
        return new User(po.getId(), loginName(po), po.getNickname(), po.getStatus(), po.getTenantId(),
                readJson(po.getAdditionalInfo()), po.getCreateTime(), po.getCreateBy(), po.getUpdateTime(),
                po.getUpdateBy());
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
