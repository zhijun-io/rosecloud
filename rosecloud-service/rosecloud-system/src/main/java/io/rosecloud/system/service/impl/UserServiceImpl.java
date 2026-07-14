package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.starter.security.session.LoginSessionApi;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.UserDao;
import io.rosecloud.system.persistence.UserEntity;
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
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService, UserApi {

    private final UserDao userDao;
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
        if (userDao.existsByUsername(request.username())) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, request.username(), request.nickname(), 1, targetTenant, null);
        User saved = userDao.insert(user);
        credentialApi.setPassword(saved.getId(), new CredentialSetRequest(request.password()));

        eventPublisher.publish(EntityChangedEvent.created("user", saved.getId(), targetTenant, saved));
        return saved.getId();
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
        if (userDao.existsByUsername(username)) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, username, nickname, 0, TenantIdSupport.requireValid(tenantId), null);
        User saved = userDao.insert(user);

        eventPublisher.publish(EntityChangedEvent.created("user", saved.getId(), tenantId, saved));
        return saved.getId();
    }

    @Override
    public PagedData<User> page(PageQuery pageQuery) {
        return userDao.page(pageQuery,
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
        return userDao.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
    }

    @AuditLog(action = "user-delete", description = "删除用户")
    @Override
    @Transactional
    public void delete(Long id) {
        userDao.findById(id).ifPresent(user -> {
            userDao.deleteUserRoles(id);
            userDao.removeById(id);

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
        User user = userDao.findById(userId)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        userDao.deleteUserRoles(userId);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                userDao.insertUserRole(userId, roleId);
            }
        }
        userPermsCache.evict(userId);
        userSecurityCache.evict(user.getUsername());
        loginSessionApi.revokeByUserId(userId);

        eventPublisher.publish(EntityChangedEvent.updated("user", userId,
                TenantContextHolder.getTenantId(), null, null));
    }

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return userDao.findRoleIdsByUserId(userId);
    }

    @Override
    public UserProfile me() {
        SecurityUser securityUser = currentSecurityUser();
        User user = userDao.findById(securityUser.getUserId())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        return new UserProfile(user, userDao.findRoleCodesByUserId(securityUser.getUserId()));
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

    /**
     * 加载用户安全信息。优先从二级缓存获取，未命中时构建并回填。
     * 缓存键为 loginName（邮箱/手机号），因该查询需 join 约 5 张表，
     * 在登录频繁时收益显著。
     */
    private Optional<SecurityUser> loadByUsernameInternal(String username) {
        return Optional.ofNullable(userSecurityCache.getOrLoad(username, () -> {
            User user = userDao.findByEmailOrPhone(username).orElse(null);
            if (user == null) {
                return null;
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : userDao.loadRoleCodes(user.getId())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            for (String perm : userDao.loadPerms(user.getId())) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }

            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getUsername());
            return new SecurityUser(
                    user.getId(), user.getUsername(), user.getUsername(), null,
                    user.getStatus() != null && user.getStatus() == 1,
                    user.getTenantId(), principal, authorities);
        }));
    }
}
