package io.rosecloud.starter.sequence;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed {@link SequenceGenerator}: {@code INCR key} returns the next
 * positive long, initializing to 1 on first use and persisting across restarts.
 * Provides cross-instance monotonic sequences; active only when a Redis client
 * is present and {@code rosecloud.sequence.type=redis}.
 */
public class RedisSequenceGenerator implements SequenceGenerator {

    private final StringRedisTemplate redis;

    public RedisSequenceGenerator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long next(String key) {
        Long value = redis.opsForValue().increment(key);
        if (value == null) {
            throw new IllegalStateException("Redis INCR returned null for key: " + key);
        }
        return value;
    }
}
