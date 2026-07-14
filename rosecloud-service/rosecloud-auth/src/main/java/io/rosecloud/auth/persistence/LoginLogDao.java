package io.rosecloud.auth.persistence;

import io.rosecloud.auth.domain.LoginLog;
import io.rosecloud.starter.data.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

@Repository
public class LoginLogDao extends MyBatisDao<LoginLog, Long, LoginLogEntity> {

    public LoginLogDao(LoginLogMapper loginLogMapper) {
        super(loginLogMapper, LoginLogEntity.class);
    }

    @Override
    protected Long getId(LoginLog domain) {
        return domain.getId();
    }

    @Override
    protected LoginLogEntity toEntity(LoginLog domain) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setUsername(domain.getUsername());
        entity.setSuccess(domain.isSuccess() ? 1 : 0);
        entity.setFailReason(domain.getFailReason());
        entity.setIp(domain.getIp());
        entity.setUserAgent(domain.getUserAgent());
        entity.setDeviceId(domain.getDeviceId());
        entity.setLoginTime(domain.getLoginTime());
        return entity;
    }
}
