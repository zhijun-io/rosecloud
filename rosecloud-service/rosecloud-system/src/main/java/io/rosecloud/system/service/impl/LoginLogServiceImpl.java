package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.api.log.LoginLogApi;
import io.rosecloud.api.log.LoginLogRequest;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.LoginLog;
import io.rosecloud.system.persistence.LoginLogEntity;
import io.rosecloud.system.persistence.LoginLogMapper;
import io.rosecloud.system.service.LoginLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
        po.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(po);
    }

    @Override
    public PageResult<LoginLog> page(long current, long size, String username, Boolean success) {
        Page<LoginLogEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<LoginLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.eq(LoginLogEntity::getUsername, username);
        }
        if (success != null) {
            wrapper.eq(LoginLogEntity::getSuccess, success ? 1 : 0);
        }
        wrapper.orderByDesc(LoginLogEntity::getLoginTime);
        IPage<LoginLogEntity> result = loginLogMapper.selectPage(page, wrapper);
        List<LoginLog> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private LoginLog toDomain(LoginLogEntity po) {
        return new LoginLog(po.getId(), po.getUsername(), po.getSuccess() != null && po.getSuccess() == 1,
                po.getFailReason(), po.getIp(), po.getUserAgent(), po.getLoginTime());
    }
}
