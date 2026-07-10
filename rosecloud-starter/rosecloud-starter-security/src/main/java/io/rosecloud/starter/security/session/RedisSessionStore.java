package io.rosecloud.starter.security.session;

import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.JwtClaimsExtractor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis-backed {@link SessionStore}. Each session is stored as a Redis Hash
 * keyed by {@code session:&lt;sessionId&gt;}. Token {@code jti}s that have been
 * revoked are stored under {@code revoked:&lt;jti&gt;} with a TTL equal to the
 * token's remaining lifetime, and {@link #isRevoked(String)} consults only that
 * set — so token validity is independent of whether a session record exists.
 *
 * <p>Because the revocation set is shared, logout / rotation / user-disable
 * propagate to every service that points at the same Redis. Session records
 * (for audit / admin listing / cascade revocation) live under
 * {@code session:*} and use a TTL of {@code min(expireAt, configured max)}.
 */
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "session:";
    private static final String ALL_INDEX_KEY = KEY_PREFIX + "ids";
    private static final String TOKEN_INDEX_PREFIX = KEY_PREFIX + "token:";
    private static final String USER_INDEX_PREFIX = KEY_PREFIX + "user:";
    private static final String REVOKED_PREFIX = "revoked:";
    private static final Duration MAX_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisSessionStore(StringRedisTemplate redisTemplate) {
        this(redisTemplate, Duration.ofDays(7));
    }

    public RedisSessionStore(StringRedisTemplate redisTemplate, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public void save(LoginSession session) {
        String sessionKey = sessionKey(session.id());
        Map<String, String> values = new HashMap<>();
        values.put("id", session.id());
        values.put("token", session.token());
        values.put("refreshToken", emptyToNullSafe(session.refreshToken()));
        values.put("userId", String.valueOf(session.userId()));
        values.put("username", session.username());
        values.put("nickname", emptyToNullSafe(session.nickname()));
        values.put("clientIp", emptyToNullSafe(session.clientIp()));
        values.put("userAgent", emptyToNullSafe(session.userAgent()));
        values.put("loginAt", session.loginAt().toString());
        values.put("expireAt", session.expireAt().toString());
        redisTemplate.opsForHash().putAll(sessionKey, values);
        redisTemplate.expire(sessionKey, sessionTtl(session.expireAt()));

        // token → sessionIds index
        redisTemplate.opsForSet().add(tokenIndexKey(session.token()), session.id());
        redisTemplate.expire(tokenIndexKey(session.token()), sessionTtl(session.expireAt()));
        redisTemplate.opsForSet().add(tokenIndexKey(session.refreshToken()), session.id());
        redisTemplate.expire(tokenIndexKey(session.refreshToken()), sessionTtl(session.expireAt()));

        // user → sessionIds index
        redisTemplate.opsForSet().add(userIndexKey(session.userId()), session.id());
        redisTemplate.expire(userIndexKey(session.userId()), sessionTtl(session.expireAt()));

        // global sessionIds index for admin listing
        redisTemplate.opsForSet().add(ALL_INDEX_KEY, session.id());
        redisTemplate.expire(ALL_INDEX_KEY, MAX_TTL);
    }

    @Override
    public void revoke(String token) {
        // Add the token's jti to the shared revocation set so every service rejects it.
        addToRevocationSet(token);
        // Cascade to the paired token and drop the session record (admin visibility).
        Set<String> sessionIds = redisTemplate.opsForSet().members(tokenIndexKey(token));
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
        Set<String> sessionIds = redisTemplate.opsForSet().members(userIndexKey(userId));
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

    private void revokeSession(String sessionId) {
        LoginSession session = readSession(sessionId);
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

    private LoginSession readSession(String sessionId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return new LoginSession(
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
            );
        } catch (RuntimeException ex) {
            return null;
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

    private static String sessionKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private static String tokenIndexKey(String token) {
        return KEY_PREFIX + "token:" + token;
    }

    private static String userIndexKey(Long userId) {
        return KEY_PREFIX + "user:" + userId;
    }

    private static String revokedKey(String jti) {
        return REVOKED_PREFIX + jti;
    }

    private void removeTokenIndex(String token, String sessionId) {
        if (token != null && !token.isBlank()) {
            redisTemplate.opsForSet().remove(tokenIndexKey(token), sessionId);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String emptyToNullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
