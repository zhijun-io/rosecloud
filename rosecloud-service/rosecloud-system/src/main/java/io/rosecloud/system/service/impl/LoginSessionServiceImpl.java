package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.system.service.LoginSessionService;
import io.rosecloud.system.persistence.LoginSessionEntity;
import io.rosecloud.system.persistence.LoginSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis-Plus backed {@link SessionStore}. Handles the mapping between the
 * {@link LoginSession} domain record and the {@link LoginSessionEntity}
 * persistent object.
 *
 * <p>The {@code sessionId} field on {@link LoginSession} doubles as the
 * database primary key (UUID, manually assigned by the authentication
 * success handler).
 *
 * <p>Extra query methods ({@link #findBySessionId}, {@link #findByUserId},
 * {@link #findAll}) are provided alongside the {@link SessionStore} contract
 * so that admin controllers can list and inspect sessions.
 */
@Service
@Transactional
public class LoginSessionServiceImpl implements LoginSessionService {

    private final LoginSessionMapper mapper;

    public LoginSessionServiceImpl(LoginSessionMapper mapper) {
        this.mapper = mapper;
    }

    // ---- SessionStore contract ----

    @Override
    public void save(LoginSession session) {
        mapper.insert(toEntity(session));
    }

    @Override
    public void revoke(String token) {
        mapper.delete(new LambdaQueryWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getToken, token));
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        mapper.deleteById(sessionId);
    }

    @Override
    public void revokeByUserId(Long userId) {
        mapper.delete(new LambdaQueryWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getUserId, userId));
    }

    @Override
    public boolean isRevoked(String token) {
        return mapper.selectCount(new LambdaQueryWrapper<LoginSessionEntity>()
                .eq(LoginSessionEntity::getToken, token)) == 0;
    }

    // ---- Extended query methods (beyond SessionStore) ----

    public Optional<LoginSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(mapper.selectById(sessionId))
                .map(this::toDomain);
    }

    public Optional<LoginSession> findByToken(String token) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<LoginSessionEntity>()
                        .eq(LoginSessionEntity::getToken, token)
                        .last("LIMIT 1")))
                .map(this::toDomain);
    }

    public List<LoginSession> findByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<LoginSessionEntity>()
                        .eq(LoginSessionEntity::getUserId, userId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    public List<LoginSession> findAll() {
        return mapper.selectList(null)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ---- Mapping helpers ----

    private static LoginSessionEntity toEntity(LoginSession session) {
        LoginSessionEntity entity = new LoginSessionEntity();
        entity.setId(session.id());
        entity.setToken(session.token());
        entity.setUserId(session.userId());
        entity.setUsername(session.username());
        entity.setNickname(session.nickname());
        entity.setClientIp(session.clientIp());
        entity.setUserAgent(session.userAgent());
        entity.setLoginAt(toLocalDateTime(session.loginAt()));
        entity.setExpireAt(toLocalDateTime(session.expireAt()));
        return entity;
    }

    private LoginSession toDomain(LoginSessionEntity entity) {
        return new LoginSession(
                entity.getId(),
                entity.getToken(),
                entity.getUserId(),
                entity.getUsername(),
                entity.getNickname(),
                entity.getClientIp(),
                entity.getUserAgent(),
                toInstant(entity.getLoginAt()),
                toInstant(entity.getExpireAt())
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null
                ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                : null;
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null
                ? localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                : null;
    }
}
