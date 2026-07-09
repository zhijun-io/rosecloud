package io.rosecloud.system.service.impl;

import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.system.service.LoginSessionService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Redis-backed {@link LoginSessionService}. Uses the same key layout as the
 * security starter so auth and system can share the same session state.
 */
@Service
public class LoginSessionServiceImpl implements LoginSessionService {

    private static final String KEY_PREFIX = "session:";
    private static final String ALL_INDEX_KEY = KEY_PREFIX + "ids";
    private static final String TOKEN_INDEX_PREFIX = KEY_PREFIX + "token:";
    private static final String USER_INDEX_PREFIX = KEY_PREFIX + "user:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public LoginSessionServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(LoginSession session) {
        String sessionKey = sessionKey(session.id());
        redisTemplate.opsForHash().putAll(sessionKey, Map.of(
                "id", session.id(),
                "token", session.token(),
                "userId", String.valueOf(session.userId()),
                "username", session.username(),
                "nickname", nullToEmpty(session.nickname()),
                "clientIp", nullToEmpty(session.clientIp()),
                "userAgent", nullToEmpty(session.userAgent()),
                "loginAt", session.loginAt().toString(),
                "expireAt", session.expireAt().toString()
        ));
        redisTemplate.expire(sessionKey, TTL);
        redisTemplate.opsForSet().add(ALL_INDEX_KEY, session.id());
        redisTemplate.expire(ALL_INDEX_KEY, TTL);
        redisTemplate.opsForSet().add(tokenIndexKey(session.token()), session.id());
        redisTemplate.expire(tokenIndexKey(session.token()), TTL);
        redisTemplate.opsForSet().add(userIndexKey(session.userId()), session.id());
        redisTemplate.expire(userIndexKey(session.userId()), TTL);
    }

    @Override
    public void revoke(String token) {
        Set<String> sessionIds = safeMembers(tokenIndexKey(token));
        if (sessionIds != null) {
            sessionIds.forEach(this::deleteSession);
        }
        redisTemplate.delete(tokenIndexKey(token));
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        deleteSession(sessionId);
    }

    @Override
    public void revokeByUserId(Long userId) {
        Set<String> sessionIds = safeMembers(userIndexKey(userId));
        if (sessionIds != null) {
            sessionIds.forEach(this::deleteSession);
        }
        redisTemplate.delete(userIndexKey(userId));
    }

    @Override
    public boolean isRevoked(String token) {
        Set<String> sessionIds = safeMembers(tokenIndexKey(token));
        return sessionIds == null || sessionIds.isEmpty();
    }

    @Override
    public Optional<LoginSession> findBySessionId(String sessionId) {
        return readSession(sessionId);
    }

    @Override
    public Optional<LoginSession> findByToken(String token) {
        Set<String> sessionIds = safeMembers(tokenIndexKey(token));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Optional.empty();
        }
        return sessionIds.stream()
                .map(this::readSession)
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public List<LoginSession> findByUserId(Long userId) {
        Set<String> sessionIds = safeMembers(userIndexKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .map(this::readSession)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<LoginSession> findAll() {
        Set<String> sessionIds = safeMembers(ALL_INDEX_KEY);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .map(this::readSession)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<LoginSession> readSession(String sessionId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new LoginSession(
                    sessionId,
                    stringValue(values.get("token")),
                    Long.valueOf(stringValue(values.get("userId"))),
                    stringValue(values.get("username")),
                    emptyToNull(stringValue(values.get("nickname"))),
                    emptyToNull(stringValue(values.get("clientIp"))),
                    emptyToNull(stringValue(values.get("userAgent"))),
                    Instant.parse(stringValue(values.get("loginAt"))),
                    Instant.parse(stringValue(values.get("expireAt")))
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private void deleteSession(String sessionId) {
        readSession(sessionId).ifPresent(session -> {
            redisTemplate.opsForSet().remove(tokenIndexKey(session.token()), sessionId);
            redisTemplate.opsForSet().remove(userIndexKey(session.userId()), sessionId);
            redisTemplate.opsForSet().remove(ALL_INDEX_KEY, sessionId);
        });
        redisTemplate.delete(sessionKey(sessionId));
    }

    private Set<String> safeMembers(String key) {
        return redisTemplate.opsForSet().members(key);
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
