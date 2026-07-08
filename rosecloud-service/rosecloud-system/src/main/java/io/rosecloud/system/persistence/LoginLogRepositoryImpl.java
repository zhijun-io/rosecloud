package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.LoginLog;
import io.rosecloud.system.domain.LoginLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LoginLogRepositoryImpl implements LoginLogRepository {

    private final LoginLogMapper mapper;

    public LoginLogRepositoryImpl(LoginLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(LoginLog log) {
        LoginLogEntity po = new LoginLogEntity();
        po.setUsername(log.getUsername());
        po.setSuccess(log.isSuccess() ? 1 : 0);
        po.setFailReason(log.getFailReason());
        po.setIp(log.getIp());
        po.setUserAgent(log.getUserAgent());
        po.setLoginTime(log.getLoginTime());
        mapper.insert(po);
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
        IPage<LoginLogEntity> result = mapper.selectPage(page, wrapper);
        List<LoginLog> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private LoginLog toDomain(LoginLogEntity po) {
        return new LoginLog(po.getId(), po.getUsername(), po.getSuccess() != null && po.getSuccess() == 1,
                po.getFailReason(), po.getIp(), po.getUserAgent(), po.getLoginTime());
    }
}
