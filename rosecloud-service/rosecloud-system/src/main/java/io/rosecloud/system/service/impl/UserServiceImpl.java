package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserPasswordUpdateRequest;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;
import io.rosecloud.system.support.PasswordPolicyValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
public class UserServiceImpl implements UserService, SystemUserApi, Function<String, Optional<SecurityUser>> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (userRepository.existsByUsername(request.username())) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        PasswordPolicyValidator.validate(request.password());
        User user = new User(null, request.username(), request.nickname(), 1, request.tenantId(), null);
        return userRepository.insert(user, passwordEncoder.encode(request.password()));
    }

    @Override
    @Transactional
    public Long createWithHash(String username, String passwordHash, String nickname, String tenantId) {
        if (userRepository.existsByUsername(username)) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, username, nickname, 1, tenantId, null);
        return userRepository.insert(user, passwordHash);
    }

    @Override
    @Transactional
    public Long createWithoutPassword(String username, String nickname, String tenantId) {
        if (userRepository.existsByUsername(username)) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, username, nickname, 0, tenantId, null);
        return userRepository.insert(user, null);
    }

    @Override
    public PageResult<User> page(long current, long size, String keyword) {
        return userRepository.page(current, size, keyword);
    }

    @Override
    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
    }

    @AuditLog(action = "user-delete", description = "删除用户")
    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<SecurityUser> loadByUsername(String username) {
        return userRepository.loadByUsername(username);
    }

    @Override
    public ApiResponse<SecurityUser> loadUserByUsername(String username) {
        return ApiResponse.ok(loadByUsername(username).orElse(null));
    }

    @Override
    public Optional<SecurityUser> apply(String username) {
        return loadByUsername(username);
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
    }

    @AuditLog(action = "user-assign-roles", description = "用户角色授权")
    @Override
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new BizException(SystemErrorCode.USER_NOT_FOUND);
        }
        userRepository.assignRoles(userId, roleIds == null ? List.of() : roleIds);
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

    @Override
    public void updateLastLoginTime(Long userId, LocalDateTime lastLoginTime) {
        userRepository.updateLastLoginTime(userId, lastLoginTime);
    }

    @Override
    @Transactional
    public ApiResponse<Void> updatePassword(Long userId, UserPasswordUpdateRequest request) {
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
        return ApiResponse.ok();
    }

}
