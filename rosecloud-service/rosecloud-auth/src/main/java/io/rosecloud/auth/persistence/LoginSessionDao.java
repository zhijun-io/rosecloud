package io.rosecloud.auth.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.security.model.LoginSession;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * DAO for login sessions. Does not extend {@code MyBatisDao} because
 * {@link LoginSessionEntity} does not implement {@code ToData<LoginSession>}
 * and the conversion between entity and record is non-trivial.
 */
@Repository
public class LoginSessionDao {

    private final LoginSessionMapper mapper;

    public LoginSessionDao(LoginSessionMapper mapper) {
        this.mapper = mapper;
    }

    public void insert(LoginSession session) {
        LoginSessionEntity e = toEntity(session);
        mapper.insert(e);
    }

    public long countActiveByToken(String token) {
        if (token == null || token.isBlank()) {
            return 0;
        }
        return mapper.selectCount(new LambdaQueryWrapper<LoginSessionEntity>()
                .and(w -> w.eq(LoginSessionEntity::getToken, token)
                        .or().eq(LoginSessionEntity::getRefreshToken, token))
                .eq(LoginSessionEntity::getRevoked, 1)
                .eq(LoginSessionEntity::getDeleted, 0));
    }

    public Optional<LoginSession> findBySessionId(String sessionId) {
        return findOne(activeBySessionId(sessionId));
    }

    public Optional<LoginSession> findByToken(String token) {
        return findOne(new LambdaQueryWrapper<LoginSessionEntity>()
                .and(w -> w.eq(LoginSessionEntity::getToken, token)
                        .or().eq(LoginSessionEntity::getRefreshToken, token))
                .eq(LoginSessionEntity::getRevoked, 0)
                .eq(LoginSessionEntity::getDeleted, 0));
    }

    public List<LoginSession> findByUserId(Long userId) {
        return mapper.selectList(activeByUserId(userId))
                .stream().map(this::toLoginSession).toList();
    }

    public List<LoginSession> findAllActive() {
        return mapper.selectList(new LambdaQueryWrapper<LoginSessionEntity>()
                        .eq(LoginSessionEntity::getRevoked, 0)
                        .eq(LoginSessionEntity::getDeleted, 0)
                        .orderByDesc(LoginSessionEntity::getLoginAt))
                .stream().map(this::toLoginSession).toList();
    }

    public List<LoginSessionEntity> findActiveEntitiesByUserId(Long userId) {
        return mapper.selectList(activeByUserId(userId));
    }

    public void markRevokedByToken(String token) {
        LoginSessionEntity e = new LoginSessionEntity();
        e.setRevoked(1);
        mapper.update(e, new LambdaQueryWrapper<LoginSessionEntity>()
                .and(w -> w.eq(LoginSessionEntity::getToken, token)
                        .or().eq(LoginSessionEntity::getRefreshToken, token))
                .eq(LoginSessionEntity::getRevoked, 0));
    }

    public void markRevokedBySessionId(String sessionId) {
        LoginSessionEntity e = new LoginSessionEntity();
        e.setRevoked(1);
        mapper.update(e, activeBySessionId(sessionId));
    }

    public void markRevokedByUserId(Long userId) {
        LoginSessionEntity e = new LoginSessionEntity();
        e.setRevoked(1);
        mapper.update(e, activeByUserId(userId));
    }

    private Optional<LoginSession> findOne(LambdaQueryWrapper<LoginSessionEntity> wrapper) {
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toLoginSession);
    }

    private LambdaQueryWrapper<LoginSessionEntity> activeBySessionId(String sessionId) {
        return new LambdaQueryWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getSessionId, sessionId)
                .eq(LoginSessionEntity::getRevoked, 0)
                .eq(LoginSessionEntity::getDeleted, 0);
    }

    private LambdaQueryWrapper<LoginSessionEntity> activeByUserId(Long userId) {
        return new LambdaQueryWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getUserId, userId)
                .eq(LoginSessionEntity::getRevoked, 0)
                .eq(LoginSessionEntity::getDeleted, 0)
                .orderByAsc(LoginSessionEntity::getLoginAt);
    }

    private LoginSession toLoginSession(LoginSessionEntity e) {
        return new LoginSession(
                e.getSessionId(), e.getToken(), e.getRefreshToken(), e.getUserId(), e.getUsername(),
                e.getNickname(), e.getClientIp(), e.getUserAgent(),
                e.getLoginAt() == null ? null : e.getLoginAt().toInstant(ZoneOffset.UTC),
                e.getExpireAt() == null ? null : e.getExpireAt().toInstant(ZoneOffset.UTC),
                e.getDeviceId());
    }

    private LoginSessionEntity toEntity(LoginSession session) {
        LoginSessionEntity e = new LoginSessionEntity();
        e.setSessionId(session.id());
        e.setUserId(session.userId());
        e.setUsername(session.username());
        e.setNickname(session.nickname());
        e.setToken(session.token());
        e.setRefreshToken(session.refreshToken());
        e.setClientIp(session.clientIp());
        e.setUserAgent(session.userAgent());
        e.setDeviceId(session.deviceId());
        e.setLoginAt(session.loginAt() == null ? null : LocalDateTime.ofInstant(session.loginAt(), ZoneOffset.UTC));
        e.setExpireAt(session.expireAt() == null ? null : LocalDateTime.ofInstant(session.expireAt(), ZoneOffset.UTC));
        e.setRevoked(0);
        return e;
    }
}
