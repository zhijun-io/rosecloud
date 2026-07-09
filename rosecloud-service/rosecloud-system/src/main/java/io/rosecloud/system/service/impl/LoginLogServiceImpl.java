package io.rosecloud.system.service.impl;

import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.LoginLog;
import io.rosecloud.system.domain.LoginLogRepository;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogServiceImpl implements LoginLogService, LoginLogApi {

    private final LoginLogRepository loginLogRepository;

    public LoginLogServiceImpl(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
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
}
