package io.rosecloud.system.service.impl;

import io.rosecloud.api.user.UserApi;
import io.rosecloud.api.user.UserPasswordUpdateRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;
import io.rosecloud.system.support.PasswordPolicyValidator;
import io.rosecloud.system.support.TenantIdSupport;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService, UserApi {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessionStore;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, SessionStore sessionStore) {
        this.userRepository = userRepository;
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
        if (userRepository.existsByUsername(request.username())) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        PasswordPolicyValidator.validate(request.password());
        User user = new User(null, request.username(), request.nickname(), 1, targetTenant, null);
        return userRepository.insert(user, passwordEncoder.encode(request.password()));
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
        if (userRepository.existsByUsername(username)) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, username, nickname, 1, TenantIdSupport.requireValid(tenantId), null);
        return userRepository.insert(user, passwordHash);
    }

    @Override
    @Transactional
    public Long createWithoutPassword(String username, String nickname, String tenantId) {
        if (userRepository.existsByUsername(username)) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, username, nickname, 0, TenantIdSupport.requireValid(tenantId), null);
        return userRepository.insert(user, null);
    }

    @Override
    public PageResult<User> page(long current, long size, String keyword) {
        return userRepository.page(current, size, keyword);
    }

    @Override
    public User get(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @AuditLog(action = "user-delete", description = "删除用户")
    @Override
    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public SecurityUser loadByUsername(String username) {
        return userRepository.loadByUsername(username).orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        SecurityUser securityUser = currentSecurityUser();
        SecurityUser authInfo = userRepository.loadByUsername(securityUser.getUsername())
                .orElseThrow(() -> new BizException(SecurityErrorCode.UNAUTHORIZED));
        if (authInfo.getPassword() == null || authInfo.getPassword().isBlank()
                || !passwordEncoder.matches(request.currentPassword(), authInfo.getPassword())) {
            throw new BizException(SecurityErrorCode.BAD_CREDENTIALS);
        }
        PasswordPolicyValidator.validateChange(request.currentPassword(), request.newPassword());
        userRepository.updatePassword(securityUser.getUserId(),
                passwordEncoder.encode(request.newPassword()), LocalDateTime.now());
        // The password is embedded in the JWT; revoke existing sessions so a previously
        // valid token (e.g. one held by an attacker before the change) cannot be reused.
        sessionStore.revokeByUserId(securityUser.getUserId());
    }

    @AuditLog(action = "user-assign-roles", description = "用户角色授权")
    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new BizException(SystemErrorCode.USER_NOT_FOUND);
        }
        userRepository.assignRoles(userId, roleIds == null ? List.of() : roleIds);
        // Roles/permissions are cached in the JWT; revoke the user's sessions so the next
        // request is forced to re-authenticate with the updated authority set.
        sessionStore.revokeByUserId(userId);
    }

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return userRepository.findRoleIdsByUserId(userId);
    }

    @Override
    public UserProfile me() {
        SecurityUser securityUser = currentSecurityUser();
        User user = userRepository.findById(securityUser.getUserId())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        return new UserProfile(user, userRepository.findRoleCodesByUserId(securityUser.getUserId()));
    }

    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequest request) {
        SecurityUser securityUser = currentSecurityUser();
        if (!securityUser.getUserId().equals(userId)) {
            throw new BizException(SecurityErrorCode.UNAUTHORIZED);
        }
        SecurityUser authInfo = userRepository.loadByUsername(securityUser.getUsername())
                .orElseThrow(() -> new BizException(SecurityErrorCode.UNAUTHORIZED));
        if (authInfo.getPassword() == null || authInfo.getPassword().isBlank()
                || !passwordEncoder.matches(request.currentPassword(), authInfo.getPassword())) {
            throw new BizException(SecurityErrorCode.BAD_CREDENTIALS);
        }
        PasswordPolicyValidator.validateChange(request.currentPassword(), request.newPassword());
        userRepository.updatePassword(userId, passwordEncoder.encode(request.newPassword()), LocalDateTime.now());
        sessionStore.revokeByUserId(userId);
    }

    @Override
    public SecurityUser loadUserByUsername(String username) {
        return userRepository.loadByUsername(username).orElse(null);
    }

}
