package io.rosecloud.auth.service.impl;

import io.rosecloud.auth.service.LoginSessionService;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.token.JwtClaimsExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Redis-backed {@link LoginSessionService}. Shares the same key layout as the
 * security starter so auth can share session state. Token validity is decoupled
 * from session presence: {@link #isRevoked(String)} consults a shared revocation
 * set keyed by the token's {@code jti} (TTL = token remaining life), so logout,
 * refresh-token rotation and user-disable propagate to every service pointing at
 * the same Redis, and a valid token is accepted even after a restart.
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

    @Override
    public void save(LoginSession session) {
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
        values.put("loginAt", session.loginAt().toString());
        values.put("expireAt", session.expireAt().toString());
        redisTemplate.opsForHash().putAll(sessionKey, values);
        Duration ttl = sessionTtl(session.expireAt());
        redisTemplate.expire(sessionKey, ttl);
        redisTemplate.opsForSet().add(ALL_INDEX_KEY, session.id());
        redisTemplate.expire(ALL_INDEX_KEY, MAX_TTL);
        redisTemplate.opsForSet().add(tokenIndexKey(session.token()), session.id());
        redisTemplate.expire(tokenIndexKey(session.token()), ttl);
        if (session.refreshToken() != null && !session.refreshToken().isBlank()) {
            redisTemplate.opsForSet().add(tokenIndexKey(session.refreshToken()), session.id());
            redisTemplate.expire(tokenIndexKey(session.refreshToken()), ttl);
        }
        redisTemplate.opsForSet().add(userIndexKey(session.userId()), session.id());
        redisTemplate.expire(userIndexKey(session.userId()), ttl);
    }

    @Override
    public void revoke(String token) {
        addToRevocationSet(token);
        Set<String> sessionIds = safeMembers(tokenIndexKey(token));
        if (sessionIds != null) {
            sessionIds.forEach(this::revokeSession);
        }
        redisTemplate.delete(tokenIndexKey(token));
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        revokeSession(sessionId);
    }

    @Override
    public void revokeByUserId(Long userId) {
        Set<String> sessionIds = safeMembers(userIndexKey(userId));
        if (sessionIds != null) {
            sessionIds.forEach(this::revokeSession);
        }
        redisTemplate.delete(userIndexKey(userId));
    }

    @Override
    public boolean isRevoked(String token) {
        return JwtClaimsExtractor.extract(token)
                .map(target -> Boolean.TRUE.equals(redisTemplate.hasKey(revokedKey(target.jti()))))
                .orElse(false);
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

    private void revokeSession(String sessionId) {
        LoginSession session = readSession(sessionId).orElse(null);
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

    private Optional<LoginSession> readSession(String sessionId) {
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
                    Instant.parse(stringValue(values.get("expireAt")))
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
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

    private static String revokedKey(String jti) {
        return REVOKED_PREFIX + jti;
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

    private void removeTokenIndex(String token, String sessionId) {
        if (token != null && !token.isBlank()) {
            redisTemplate.opsForSet().remove(tokenIndexKey(token), sessionId);
        }
    }
}
