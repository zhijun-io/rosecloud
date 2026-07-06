package io.rosecloud.starter.security.jwt;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis-backed {@link TokenRevocationService}: stores {@code rosecloud:revoked:{jti}}
 * with a TTL matching the token's remaining lifetime, so revocations are shared
 * across instances and self-expire. Active only when
 * {@code rosecloud.security.token-revocation.type=redis} and a Redis client is
 * on the classpath.
 */
public class RedisTokenRevocationService implements TokenRevocationService {

    private static final String KEY_PREFIX = "rosecloud:revoked:";

    private final StringRedisTemplate redis;

    public RedisTokenRevocationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        long ttlSeconds = expiresAt == null
                ? 3600L
                : Math.max(1L, Duration.between(Instant.now(), expiresAt).getSeconds());
        redis.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
    }
}
