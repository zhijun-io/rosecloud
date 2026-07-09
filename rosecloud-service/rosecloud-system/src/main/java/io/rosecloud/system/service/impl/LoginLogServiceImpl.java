package io.rosecloud.system.service.impl;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.api.user.UserApi;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.system.domain.LoginLog;
import io.rosecloud.system.domain.LoginLogRepository;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogServiceImpl implements LoginLogService, LoginLogApi, UserApi {

    private final LoginLogRepository loginLogRepository;
    private final UserRepository userRepository;

    public LoginLogServiceImpl(LoginLogRepository loginLogRepository, UserRepository userRepository) {
        this.loginLogRepository = loginLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void record(LoginLogRequest request) {
        loginLogRepository.insert(new LoginLog(null, request.username(), request.success(),
                request.failReason(), request.ip(), request.userAgent(), LocalDateTime.now()));
    }

    @Override
    public PageResult<LoginLog> page(long current, long size, String username, Boolean success) {
        return loginLogRepository.page(current, size, username, success);
    }

    @Override
    public SecurityUser loadUserByUsername(String username) {
        return userRepository.loadByUsername(username).orElse(null);
    }
}
