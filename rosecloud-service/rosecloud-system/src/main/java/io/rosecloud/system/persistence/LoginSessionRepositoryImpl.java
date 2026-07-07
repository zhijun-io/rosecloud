package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.LoginSession;
import io.rosecloud.system.domain.LoginSessionRepository;
import io.rosecloud.system.domain.LoginSessionStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class LoginSessionRepositoryImpl implements LoginSessionRepository {

    private final LoginSessionMapper mapper;

    public LoginSessionRepositoryImpl(LoginSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Long insert(LoginSession session) {
        LoginSessionPO po = toPO(session);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public Optional<LoginSession> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<LoginSession> findByJti(String jti) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<LoginSessionPO>().eq(LoginSessionPO::getJti, jti))).map(this::toDomain);
    }

    @Override
    public void markLoggedOutByJti(String jti) {
        mapper.update(null, new LambdaUpdateWrapper<LoginSessionPO>()
                .eq(LoginSessionPO::getJti, jti)
                .set(LoginSessionPO::getStatus, LoginSessionStatus.LOGGED_OUT.code())
                .set(LoginSessionPO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public void markLoggedOutById(Long id) {
        mapper.update(null, new LambdaUpdateWrapper<LoginSessionPO>()
                .eq(LoginSessionPO::getId, id)
                .set(LoginSessionPO::getStatus, LoginSessionStatus.LOGGED_OUT.code())
                .set(LoginSessionPO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public PageResult<LoginSession> onlinePage(long current, long size, Long tenantId, LocalDateTime now) {
        Page<LoginSessionPO> page = new Page<>(current, size);
        LambdaQueryWrapper<LoginSessionPO> wrapper = new LambdaQueryWrapper<LoginSessionPO>()
                .eq(LoginSessionPO::getStatus, LoginSessionStatus.ONLINE.code())
                .gt(LoginSessionPO::getExpireTime, now);
        if (tenantId != null) {
            wrapper.eq(LoginSessionPO::getTenantId, tenantId);
        }
        wrapper.orderByDesc(LoginSessionPO::getLoginTime);
        IPage<LoginSessionPO> result = mapper.selectPage(page, wrapper);
        List<LoginSession> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private LoginSession toDomain(LoginSessionPO po) {
        return new LoginSession(po.getId(), po.getJti(), po.getUserId(), po.getUsername(), po.getTenantId(),
                po.getLoginTime(), po.getExpireTime(), po.getIp(), po.getUserAgent(), po.getStatus());
    }

    private LoginSessionPO toPO(LoginSession s) {
        LoginSessionPO po = new LoginSessionPO();
        po.setId(s.id());
        po.setJti(s.jti());
        po.setUserId(s.userId());
        po.setUsername(s.username());
        po.setTenantId(s.tenantId());
        po.setLoginTime(s.loginTime());
        po.setExpireTime(s.expireTime());
        po.setIp(s.ip());
        po.setUserAgent(s.userAgent());
        po.setStatus(s.status());
        return po;
    }
}
