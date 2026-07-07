package io.rosecloud.starter.cache;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis-backed {@link RoseCloudCache}: delegates to {@link StringRedisTemplate}.
 * {@code ttl=null} stores with no expiry; otherwise the entry expires after the
 * ttl. Provides cross-instance consistency; active only when a Redis client is
 * present and {@code rosecloud.cache.type=redis}.
 */
public class RedisRoseCloudCache implements RoseCloudCache {

    private final StringRedisTemplate redis;

    public RedisRoseCloudCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        if (ttl == null) {
            redis.opsForValue().set(key, value);
        } else {
            redis.opsForValue().set(key, value, ttl);
        }
    }

    @Override
    public void evict(String key) {
        redis.delete(key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = redis.opsForValue().increment(key);
        if (ttl != null && value != null) {
            redis.expire(key, ttl);
        }
        return value == null ? 0 : value;
    }
}
