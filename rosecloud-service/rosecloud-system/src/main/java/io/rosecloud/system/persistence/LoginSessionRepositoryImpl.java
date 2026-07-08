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
        LoginSessionEntity po = toEntity(session);
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
                new LambdaQueryWrapper<LoginSessionEntity>().eq(LoginSessionEntity::getJti, jti))).map(this::toDomain);
    }

    @Override
    public void markLoggedOutByJti(String jti) {
        mapper.update(null, new LambdaUpdateWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getJti, jti)
                .set(LoginSessionEntity::getStatus, LoginSessionStatus.LOGGED_OUT.code())
                .set(LoginSessionEntity::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public void markLoggedOutById(Long id) {
        mapper.update(null, new LambdaUpdateWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getId, id)
                .set(LoginSessionEntity::getStatus, LoginSessionStatus.LOGGED_OUT.code())
                .set(LoginSessionEntity::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public PageResult<LoginSession> onlinePage(long current, long size, String tenantId, LocalDateTime now) {
        Page<LoginSessionEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<LoginSessionEntity> wrapper = new LambdaQueryWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getStatus, LoginSessionStatus.ONLINE.code())
                .gt(LoginSessionEntity::getExpireTime, now);
        if (tenantId != null) {
            wrapper.eq(LoginSessionEntity::getTenantId, tenantId);
        }
        wrapper.orderByDesc(LoginSessionEntity::getLoginTime);
        IPage<LoginSessionEntity> result = mapper.selectPage(page, wrapper);
        List<LoginSession> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private LoginSession toDomain(LoginSessionEntity po) {
        return new LoginSession(po.getId(), po.getJti(), po.getUserId(), po.getUsername(), po.getTenantId(),
                po.getLoginTime(), po.getExpireTime(), po.getIp(), po.getUserAgent(), po.getStatus());
    }

    private LoginSessionEntity toEntity(LoginSession s) {
        LoginSessionEntity po = new LoginSessionEntity();
        po.setId(s.getId());
        po.setJti(s.getJti());
        po.setUserId(s.getUserId());
        po.setUsername(s.getUsername());
        po.setTenantId(s.getTenantId());
        po.setLoginTime(s.getLoginTime());
        po.setExpireTime(s.getExpireTime());
        po.setIp(s.getIp());
        po.setUserAgent(s.getUserAgent());
        po.setStatus(s.getStatus());
        return po;
    }
}
