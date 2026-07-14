package io.rosecloud.auth.service.impl;

import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.auth.domain.LoginLog;
import io.rosecloud.auth.persistence.LoginLogDao;
import io.rosecloud.auth.persistence.LoginLogEntity;
import io.rosecloud.auth.service.LoginLogService;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.core.model.TimePageQuery;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Primary
@Service
public class LoginLogServiceImpl implements LoginLogService, LoginLogApi {

    private final LoginLogDao loginLogDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(LoginLogRequest request) {
        LoginLog log = new LoginLog(null, request.username(), request.success(),
                request.failReason(), request.ip(), request.userAgent(), request.deviceId(), LocalDateTime.now());
        loginLogDao.save(log);
    }

    @Override
    public PagedData<LoginLog> page(TimePageQuery pageQuery, String username, Boolean success) {
        return loginLogDao.page(pageQuery,
                q -> {
                    LambdaQueryWrapper<LoginLogEntity> wrapper = new LambdaQueryWrapper<>();
                    if (username != null && !username.isBlank()) {
                        wrapper.eq(LoginLogEntity::getUsername, username);
                    }
                    if (success != null) {
                        wrapper.eq(LoginLogEntity::getSuccess, success ? 1 : 0);
                    }
                    if (q.getStartTime() != null) {
                        wrapper.ge(LoginLogEntity::getLoginTime, q.getStartTime());
                    }
                    if (q.getEndTime() != null) {
                        wrapper.le(LoginLogEntity::getLoginTime, q.getEndTime());
                    }
                    return wrapper;
                },
                SortField.of("loginTime", SortDirection.DESC));
    }
}
