package io.rosecloud.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.auth.domain.LoginLog;
import io.rosecloud.auth.persistence.LoginLogEntity;
import io.rosecloud.auth.persistence.LoginLogMapper;
import io.rosecloud.auth.service.LoginLogService;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.core.model.TimePageQuery;
import io.rosecloud.starter.data.PagedResults;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Primary
@Service
public class LoginLogServiceImpl implements LoginLogService, LoginLogApi {

    private final LoginLogMapper loginLogMapper;

    public LoginLogServiceImpl(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public void record(LoginLogRequest request) {
        LoginLogEntity po = new LoginLogEntity();
        po.setUsername(request.username());
        po.setSuccess(request.success() ? 1 : 0);
        po.setFailReason(request.failReason());
        po.setIp(request.ip());
        po.setUserAgent(request.userAgent());
        po.setDeviceId(request.deviceId());
        po.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(po);
    }

    @Override
    public PagedData<LoginLog> page(TimePageQuery pageQuery, String username, Boolean success) {
        return PagedResults.page(pageQuery, LoginLogEntity.class, loginLogMapper,
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
