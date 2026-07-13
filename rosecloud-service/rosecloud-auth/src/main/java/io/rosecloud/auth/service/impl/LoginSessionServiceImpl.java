package io.rosecloud.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.auth.persistence.LoginSessionEntity;
import io.rosecloud.auth.persistence.LoginSessionMapper;
import io.rosecloud.auth.service.LoginSessionService;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.token.JwtClaimsExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * DB-backed {@link LoginSessionService}. The {@code auth_login_session} table is the authoritative
 * source for session administration (listing / audit / cascade revocation); Redis keeps the shared
 * {@code revoked:} set so token validity stays stateless and propagates across services, exactly as
 * in {@code RedisSessionStore}. Logs in / logout / refresh therefore write both stores.
 */
@Service
@RequiredArgsConstructor
public class LoginSessionServiceImpl implements LoginSessionService {

    private static final String KEY_PREFIX = "session:";
    private static final String ALL_INDEX_KEY = KEY_PREFIX + "ids";
    private static final String TOKEN_INDEX_PREFIX = KEY_PREFIX + "token:";
    private static final String USER_INDEX_PREFIX = KEY_PREFIX + "user:";
    private static final String REVOKED_PREFIX = "revoked:";
    private static final Duration MAX_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final LoginSessionMapper sessionMapper;

    @Value("${rosecloud.auth.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Override
    public void save(LoginSession session) {
        enforceConcurrentLimit(session.userId());
        persistToDb(session);
        writeToRedis(session);
    }

    @Override
    public void revoke(String token) {
        markRevokedByTokenInDb(token);
        redisRevoke(token);
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        markRevokedBySessionIdInDb(sessionId);
        redisRevokeBySessionId(sessionId);
    }

    @Override
    public void revokeByUserId(Long userId) {
        markRevokedByUserIdInDb(userId);
        redisRevokeByUserId(userId);
    }

    @Override
    public boolean isRevoked(String token) {
        return JwtClaimsExtractor.extract(token)
                .map(target -> Boolean.TRUE.equals(redisTemplate.hasKey(revokedKey(target.jti()))))
                .orElse(false);
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

    private void writeToRedis(LoginSession session) {
        String sessionKey = sessionKey(session.id());
        Map<String, String> values = new HashMap<>();
        values.put("id", session.id());
        values.put("token", session.token());
        values.put("refreshToken", nullToEmpty(session.refreshToken()));
        values.put("userId", String.valueOf(session.userId()));
        values.put("username", session.username());
        values.put("nickname", nullToEmpty(session.nickname()));
        values.put("clientIp", nullToEmpty(session.clientIp()));
        values.put("userAgent", nullToEmpty(session.userAgent()));
        values.put("deviceId", nullToEmpty(session.deviceId()));
        values.put("loginAt", session.loginAt().toString());
        values.put("expireAt", session.expireAt().toString());
        redisTemplate.opsForHash().putAll(sessionKey, values);
        redisTemplate.expire(sessionKey, sessionTtl(session.expireAt()));

        redisTemplate.opsForSet().add(tokenIndexKey(session.token()), session.id());
        redisTemplate.expire(tokenIndexKey(session.token()), sessionTtl(session.expireAt()));
        redisTemplate.opsForSet().add(tokenIndexKey(session.refreshToken()), session.id());
        redisTemplate.expire(tokenIndexKey(session.refreshToken()), sessionTtl(session.expireAt()));

        redisTemplate.opsForSet().add(userIndexKey(session.userId()), session.id());
        redisTemplate.expire(userIndexKey(session.userId()), sessionTtl(session.expireAt()));

        redisTemplate.opsForSet().add(ALL_INDEX_KEY, session.id());
        redisTemplate.expire(ALL_INDEX_KEY, MAX_TTL);
    }

    private void redisRevoke(String token) {
        addToRevocationSet(token);
        Set<String> sessionIds = safeMembers(tokenIndexKey(token));
        if (sessionIds != null) {
            sessionIds.forEach(this::redisRevokeBySessionId);
        }
        redisTemplate.delete(tokenIndexKey(token));
    }

    private void redisRevokeBySessionId(String sessionId) {
        LoginSession session = readRedisSession(sessionId).orElse(null);
        if (session != null) {
            addToRevocationSet(session.token());
            addToRevocationSet(session.refreshToken());
        }
        removeTokenIndex(session != null ? session.token() : null, sessionId);
        removeTokenIndex(session != null ? session.refreshToken() : null, sessionId);
        if (session != null) {
            redisTemplate.opsForSet().remove(userIndexKey(session.userId()), sessionId);
        }
        redisTemplate.opsForSet().remove(ALL_INDEX_KEY, sessionId);
        redisTemplate.delete(sessionKey(sessionId));
    }

    private void redisRevokeByUserId(Long userId) {
        Set<String> sessionIds = safeMembers(userIndexKey(userId));
        if (sessionIds != null) {
            sessionIds.forEach(this::redisRevokeBySessionId);
        }
        redisTemplate.delete(userIndexKey(userId));
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

    private Optional<LoginSession> readRedisSession(String sessionId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new LoginSession(
                    sessionId,
                    stringValue(values.get("token")),
                    emptyToNull(stringValue(values.get("refreshToken"))),
                    Long.valueOf(stringValue(values.get("userId"))),
                    stringValue(values.get("username")),
                    emptyToNull(stringValue(values.get("nickname"))),
                    emptyToNull(stringValue(values.get("clientIp"))),
                    emptyToNull(stringValue(values.get("userAgent"))),
                    Instant.parse(stringValue(values.get("loginAt"))),
                    Instant.parse(stringValue(values.get("expireAt"))),
                    emptyToNull(stringValue(values.get("deviceId")))
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private void addToRevocationSet(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        JwtClaimsExtractor.extract(token).ifPresent(target -> {
            Duration t = target.expiresAt() != null
                    ? Duration.between(Instant.now(), target.expiresAt()) : MAX_TTL;
            if (t.isNegative() || t.isZero()) {
                t = Duration.ofSeconds(1);
            }
            redisTemplate.opsForValue().set(revokedKey(target.jti()), "", t);
        });
    }

    private Duration sessionTtl(Instant expireAt) {
        if (expireAt == null) {
            return MAX_TTL;
        }
        Duration t = Duration.between(Instant.now(), expireAt);
        if (t.isNegative() || t.isZero()) {
            return Duration.ofSeconds(1);
        }
        return t.compareTo(MAX_TTL) > 0 ? MAX_TTL : t;
    }

    private LoginSession toLoginSession(LoginSessionEntity e) {
        return new LoginSession(
                e.getSessionId(), e.getToken(), e.getRefreshToken(), e.getUserId(), e.getUsername(),
                e.getNickname(), e.getClientIp(), e.getUserAgent(),
                e.getLoginAt() == null ? null : e.getLoginAt().toInstant(ZoneOffset.UTC),
                e.getExpireAt() == null ? null : e.getExpireAt().toInstant(ZoneOffset.UTC),
                e.getDeviceId());
    }

    private static String sessionKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private static String tokenIndexKey(String token) {
        return TOKEN_INDEX_PREFIX + token;
    }

    private static String userIndexKey(Long userId) {
        return USER_INDEX_PREFIX + userId;
    }

    private static String revokedKey(String jti) {
        return REVOKED_PREFIX + jti;
    }

    private Set<String> safeMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    private void removeTokenIndex(String token, String sessionId) {
        if (token != null && !token.isBlank()) {
            redisTemplate.opsForSet().remove(tokenIndexKey(token), sessionId);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
