package io.rosecloud.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.auth.persistence.LoginSessionEntity;
import io.rosecloud.auth.persistence.LoginSessionMapper;
import io.rosecloud.auth.service.LoginSessionService;
import io.rosecloud.common.security.model.LoginSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * DB-backed {@link LoginSessionService}. The {@code auth_login_session} table is the
 * authoritative (and only) store for sessions and token revocation — Redis is no longer
 * used. Revocation is expressed by the {@code revoked} column, so {@link #isRevoked(String)}
 * is a single indexed lookup; {@code token}/{@code refresh_token} carry prefix indexes so the
 * per-request check stays index-backed.
 */
@Service
@RequiredArgsConstructor
public class LoginSessionServiceImpl implements LoginSessionService {

    private final LoginSessionMapper sessionMapper;

    @Value("${rosecloud.auth.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Override
    public void save(LoginSession session) {
        enforceConcurrentLimit(session.userId());
        persistToDb(session);
    }

    @Override
    public void revoke(String token) {
        markRevokedByTokenInDb(token);
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        markRevokedBySessionIdInDb(sessionId);
    }

    @Override
    public void revokeByUserId(Long userId) {
        markRevokedByUserIdInDb(userId);
    }

    @Override
    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        long count = sessionMapper.selectCount(new LambdaQueryWrapper<LoginSessionEntity>()
                .and(w -> w.eq(LoginSessionEntity::getToken, token)
                        .or().eq(LoginSessionEntity::getRefreshToken, token))
                .eq(LoginSessionEntity::getRevoked, 1)
                .eq(LoginSessionEntity::getDeleted, 0));
        return count > 0;
    }

    @Override
    public Optional<LoginSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessionMapper.selectOne(activeBySessionId(sessionId))).map(this::toLoginSession);
    }

    @Override
    public Optional<LoginSession> findByToken(String token) {
        LoginSessionEntity e = sessionMapper.selectOne(new LambdaQueryWrapper<LoginSessionEntity>()
                .and(w -> w.eq(LoginSessionEntity::getToken, token).or().eq(LoginSessionEntity::getRefreshToken, token))
                .eq(LoginSessionEntity::getRevoked, 0)
                .eq(LoginSessionEntity::getDeleted, 0));
        return Optional.ofNullable(e).map(this::toLoginSession);
    }

    @Override
    public List<LoginSession> findByUserId(Long userId) {
        return sessionMapper.selectList(activeByUserId(userId)).stream().map(this::toLoginSession).toList();
    }

    @Override
    public List<LoginSession> findAll() {
        LambdaQueryWrapper<LoginSessionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginSessionEntity::getRevoked, 0).eq(LoginSessionEntity::getDeleted, 0)
                .orderByDesc(LoginSessionEntity::getLoginAt);
        return sessionMapper.selectList(wrapper).stream().map(this::toLoginSession).toList();
    }

    private void enforceConcurrentLimit(Long userId) {
        if (maxConcurrentSessions <= 0) {
            return;
        }
        List<LoginSessionEntity> active = sessionMapper.selectList(activeByUserId(userId));
        int excess = active.size() - maxConcurrentSessions + 1;
        for (int i = 0; i < excess; i++) {
            revokeBySessionId(active.get(i).getSessionId());
        }
    }

    private void persistToDb(LoginSession session) {
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
        sessionMapper.insert(e);
    }

    private void markRevokedByTokenInDb(String token) {
        LoginSessionEntity e = new LoginSessionEntity();
        e.setRevoked(1);
        sessionMapper.update(e, new LambdaQueryWrapper<LoginSessionEntity>()
                .and(w -> w.eq(LoginSessionEntity::getToken, token).or().eq(LoginSessionEntity::getRefreshToken, token))
                .eq(LoginSessionEntity::getRevoked, 0));
    }

    private void markRevokedBySessionIdInDb(String sessionId) {
        LoginSessionEntity e = new LoginSessionEntity();
        e.setRevoked(1);
        sessionMapper.update(e, activeBySessionId(sessionId));
    }

    private void markRevokedByUserIdInDb(Long userId) {
        LoginSessionEntity e = new LoginSessionEntity();
        e.setRevoked(1);
        sessionMapper.update(e, activeByUserId(userId));
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
}
