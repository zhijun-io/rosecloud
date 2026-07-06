package io.rosecloud.system.service.impl;

import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.UserCreateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    @Override
    public Long create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BizException(SystemErrorCode.USERNAME_EXISTS);
        }
        User user = new User(null, request.username(), request.nickname(), 1, request.tenantId());
        return userRepository.insert(user, passwordEncoder.encode(request.password()));
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
}
