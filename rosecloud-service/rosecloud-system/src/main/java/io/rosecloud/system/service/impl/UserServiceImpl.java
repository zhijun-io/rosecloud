package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.rosecloud.api.credential.CredentialApi;
import io.rosecloud.api.credential.CredentialChangeRequest;
import io.rosecloud.api.credential.CredentialSetRequest;
import io.rosecloud.api.user.UserApi;
import io.rosecloud.api.user.UserPasswordUpdateRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.starter.data.EntityCacheNames;
import io.rosecloud.starter.data.PagedResults;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.starter.security.session.LoginSessionApi;
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
import io.rosecloud.system.persistence.UserEntity;
import io.rosecloud.system.persistence.UserMapper;
import io.rosecloud.system.persistence.UserRoleEntity;
import io.rosecloud.system.persistence.UserRoleMapper;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;
import io.rosecloud.system.support.TenantIdSupport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService, UserApi {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final LoginSessionApi loginSessionApi;
    private final CredentialApi credentialApi;

    // ==== 缓存 ====
    private final EntityCache<String, SecurityUser> userSecurityCache;
    private final EntityCache<Long, List<String>> userPermsCache;
    private final EntityEventPublisher eventPublisher;

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
        User user = new User(null, request.username(), request.nickname(), 1, targetTenant, null);
        Long userId = insert(user);
        credentialApi.setPassword(userId, new CredentialSetRequest(request.password()));

        eventPublisher.publish(EntityChangedEvent.created("user", userId, targetTenant, user));
        return userId;
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
    public Long createWithoutPassword(String username, String nickname, String tenantId) {
        if (existsByUsername(username)) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, username, nickname, 0, TenantIdSupport.requireValid(tenantId), null);
        Long userId = insert(user);

        eventPublisher.publish(EntityChangedEvent.created("user", userId, tenantId, user));
        return userId;
    }

    @Override
    public PagedData<User> page(PageQuery pageQuery) {
        return PagedResults.page(pageQuery, UserEntity.class, userMapper,
                q -> {
                    LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
                    if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
                        wrapper.nested(w -> w.like(UserEntity::getEmail, q.getKeyword())
                                .or()
                                .like(UserEntity::getPhone, q.getKeyword())
                                .or()
                                .like(UserEntity::getNickname, q.getKeyword()));
                    }
                    return wrapper;
                },
                SortField.of("createTime", SortDirection.DESC));
    }

    @Override
    public User get(Long id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
    }

    @AuditLog(action = "user-delete", description = "删除用户")
    @Override
    @Transactional
    public void delete(Long id) {
        findById(id).ifPresent(user -> {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, id));
            userMapper.deleteById(id);

            userPermsCache.evict(id);
            userSecurityCache.evict(user.getUsername());
            eventPublisher.publish(EntityChangedEvent.deleted("user", id,
                    TenantContextHolder.getTenantId(), user));
        });
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
        credentialApi.changePassword(securityUser.getUserId(),
                new CredentialChangeRequest(request.currentPassword(), request.newPassword()));
    }

    @AuditLog(action = "user-assign-roles", description = "用户角色授权")
    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        User user = findById(userId)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRoleEntity po = new UserRoleEntity();
                po.setUserId(userId);
                po.setRoleId(roleId);
                userRoleMapper.insert(po);
            }
        }
        // 清除该用户的权限缓存和安全缓存，使得下次请求重新加载。
        userPermsCache.evict(userId);
        userSecurityCache.evict(user.getUsername());
        // 吊销该用户的所有登录态，强制下次请求获取最新 JWT。
        loginSessionApi.revokeByUserId(userId);

        eventPublisher.publish(EntityChangedEvent.updated("user", userId,
                TenantContextHolder.getTenantId(), null, null));
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
        credentialApi.changePassword(userId,
                new CredentialChangeRequest(request.currentPassword(), request.newPassword()));
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

    private Long insert(User user) {
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
        return po.getId();
    }

    private Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(UserEntity::toData);
    }

    /**
     * 加载用户安全信息。优先从二级缓存获取，未命中时构建并回填。
     * 缓存键为 loginName（邮箱/手机号），因该查询需 join 约 5 张表，
     * 在登录频繁时收益显著。
     */
    private Optional<SecurityUser> loadByUsernameInternal(String username) {
        return Optional.ofNullable(userSecurityCache.getOrLoad(username, () -> {
            UserEntity po = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                    .and(wrapper -> wrapper.eq(UserEntity::getEmail, username)
                            .or()
                            .eq(UserEntity::getPhone, username)));
            if (po == null) {
                return null;
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : loadRoleCodes(po.getId())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            for (String perm : loadPerms(po.getId())) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }

            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, loginName(po));
            return new SecurityUser(
                    po.getId(), loginName(po), loginName(po), null,
                    po.getStatus() != null && po.getStatus() == 1,
                    po.getTenantId(), principal, authorities);
        }));
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
     * 聚合用户的权限 code。结果缓存于 userPermsCache，失效时机包括：
     * 角色分配变更、菜单权限变更。
     */
    private List<String> loadPerms(Long userId) {
        return userPermsCache.getOrLoad(userId, () -> {
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
        });
    }

    private List<String> findRoleCodesByUserId(Long userId) {
        List<Long> roleIds = findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>().in(RoleEntity::getId, roleIds))
                .stream().map(RoleEntity::getCode).toList();
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

    private String writeJson(JsonNode value) {
        return value == null || value.isNull() ? null : value.toString();
    }
}
