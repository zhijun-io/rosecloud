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
        LoginLogPO po = new LoginLogPO();
        po.setUsername(log.username());
        po.setSuccess(log.success() ? 1 : 0);
        po.setFailReason(log.failReason());
        po.setIp(log.ip());
        po.setUserAgent(log.userAgent());
        po.setLoginTime(log.loginTime());
        mapper.insert(po);
    }

    @Override
    public PageResult<LoginLog> page(long current, long size, String username, Boolean success) {
        Page<LoginLogPO> page = new Page<>(current, size);
        LambdaQueryWrapper<LoginLogPO> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.eq(LoginLogPO::getUsername, username);
        }
        if (success != null) {
            wrapper.eq(LoginLogPO::getSuccess, success ? 1 : 0);
        }
        wrapper.orderByDesc(LoginLogPO::getLoginTime);
        IPage<LoginLogPO> result = mapper.selectPage(page, wrapper);
        List<LoginLog> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private LoginLog toDomain(LoginLogPO po) {
        return new LoginLog(po.getId(), po.getUsername(), po.getSuccess() != null && po.getSuccess() == 1,
                po.getFailReason(), po.getIp(), po.getUserAgent(), po.getLoginTime());
    }
}
