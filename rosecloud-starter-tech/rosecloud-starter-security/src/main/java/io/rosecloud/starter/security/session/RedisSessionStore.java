package io.rosecloud.starter.security.session;

import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.session.SessionStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis-backed {@link SessionStore}. Each session is stored as a Redis Hash
 * keyed by {@code session:&lt;sessionId&gt;}. Index sets track the
 * token→sessionIds and user→sessionIds mappings for efficient revocation.
 *
 * <p>Revoked tokens are detected by checking whether the
 * {@code session:token:&lt;token&gt;} index is empty or absent.
 */
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "session:";
    private static final String ALL_INDEX_KEY = KEY_PREFIX + "ids";
    private static final String TOKEN_INDEX_PREFIX = KEY_PREFIX + "token:";
    private static final String USER_INDEX_PREFIX = KEY_PREFIX + "user:";

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
        values.put("userId", String.valueOf(session.userId()));
        values.put("username", session.username());
        values.put("nickname", emptyToNullSafe(session.nickname()));
        values.put("clientIp", emptyToNullSafe(session.clientIp()));
        values.put("userAgent", emptyToNullSafe(session.userAgent()));
        values.put("loginAt", session.loginAt().toString());
        values.put("expireAt", session.expireAt().toString());
        redisTemplate.opsForHash().putAll(sessionKey, values);
        redisTemplate.expire(sessionKey, ttl);

        // token → sessionIds index
        redisTemplate.opsForSet().add(tokenIndexKey(session.token()), session.id());
        redisTemplate.expire(tokenIndexKey(session.token()), ttl);

        // user → sessionIds index
        redisTemplate.opsForSet().add(userIndexKey(session.userId()), session.id());
        redisTemplate.expire(userIndexKey(session.userId()), ttl);

        // global sessionIds index for admin listing
        redisTemplate.opsForSet().add(ALL_INDEX_KEY, session.id());
        redisTemplate.expire(ALL_INDEX_KEY, ttl);
    }

    @Override
    public void revoke(String token) {
        String tokenKey = tokenIndexKey(token);
        Set<String> sessionIds = redisTemplate.opsForSet().members(tokenKey);
        if (sessionIds != null) {
            sessionIds.forEach(sid -> {
                // Remove from user index before deleting the session
                String userId = (String) redisTemplate.opsForHash().get(sessionKey(sid), "userId");
                if (userId != null) {
                    redisTemplate.opsForSet().remove(userIndexKey(Long.parseLong(userId)), sid);
                }
                redisTemplate.opsForSet().remove(ALL_INDEX_KEY, sid);
                redisTemplate.delete(sessionKey(sid));
            });
        }
        redisTemplate.delete(tokenKey);
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        String sessionKey = sessionKey(sessionId);
        String token = (String) redisTemplate.opsForHash().get(sessionKey, "token");
        String userId = (String) redisTemplate.opsForHash().get(sessionKey, "userId");

        if (token != null) {
            redisTemplate.opsForSet().remove(tokenIndexKey(token), sessionId);
        }
        if (userId != null) {
            redisTemplate.opsForSet().remove(userIndexKey(Long.parseLong(userId)), sessionId);
        }
        redisTemplate.opsForSet().remove(ALL_INDEX_KEY, sessionId);
        redisTemplate.delete(sessionKey);
    }

    @Override
    public void revokeByUserId(Long userId) {
        String userKey = userIndexKey(userId);
        Set<String> sessionIds = redisTemplate.opsForSet().members(userKey);
        if (sessionIds != null) {
            sessionIds.forEach(sid -> {
                // Remove from token index before deleting the session
                String token = (String) redisTemplate.opsForHash().get(sessionKey(sid), "token");
                if (token != null) {
                    redisTemplate.opsForSet().remove(tokenIndexKey(token), sid);
                }
                redisTemplate.opsForSet().remove(ALL_INDEX_KEY, sid);
                redisTemplate.delete(sessionKey(sid));
            });
        }
        redisTemplate.delete(userKey);
    }

    @Override
    public boolean isRevoked(String token) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(tokenIndexKey(token));
        return sessionIds == null || sessionIds.isEmpty();
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

    private static String emptyToNullSafe(String value) {
        return value == null ? "" : value;
    }
}
