package io.rosecloud.system.service.impl;

import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.security.SecurityErrorCode;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.ChangePasswordRequest;
import java.time.LocalDateTime;
import io.rosecloud.system.service.dto.UserCreateRequest;
import io.rosecloud.system.service.dto.UserProfile;
import io.rosecloud.system.support.PasswordPolicyValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
    public Optional<UserAuthInfo> findAuthInfo(String username) {
        return userRepository.findAuthInfo(username);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        CurrentUser current = UserContext.get();
        if (current == null || current.userId() == null || current.username() == null) {
            throw new BizException(SecurityErrorCode.UNAUTHORIZED);
        }
        UserAuthInfo authInfo = userRepository.findAuthInfo(current.username())
                .orElseThrow(() -> new BizException(SecurityErrorCode.UNAUTHORIZED));
        if (authInfo.passwordHash() == null || authInfo.passwordHash().isBlank()
                || !passwordEncoder.matches(request.currentPassword(), authInfo.passwordHash())) {
            throw new BizException(SecurityErrorCode.BAD_CREDENTIALS);
        }
        PasswordPolicyValidator.validateChange(request.currentPassword(), request.newPassword());
        userRepository.updatePassword(current.userId(),
                passwordEncoder.encode(request.newPassword()), java.time.LocalDateTime.now());
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
        CurrentUser current = UserContext.get();
        if (current == null || current.userId() == null) {
            throw new BizException(SecurityErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findById(current.userId())
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_NOT_FOUND));
        return new UserProfile(user, userRepository.findRoleCodesByUserId(current.userId()));
    }

    @Override
    public void updateLastLoginTime(Long userId, LocalDateTime lastLoginTime) {
        userRepository.updateLastLoginTime(userId, lastLoginTime);
    }
}
