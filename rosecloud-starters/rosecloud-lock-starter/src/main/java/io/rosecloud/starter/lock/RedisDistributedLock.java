package io.rosecloud.starter.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Redis-backed {@link DistributedLock}: acquires with {@code SET key value NX PX
 * ttl} and releases with a Lua compare-and-delete so only the owner can unlock.
 * Provides cross-instance mutual exclusion; active only when a Redis client is
 * present and {@code rosecloud.lock.type=redis}.
 */
public class RedisDistributedLock implements DistributedLock {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisDistributedLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public LockToken tryLock(String key, Duration ttl) {
        String value = UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(acquired) ? new LockToken(key, value) : null;
    }

    @Override
    public void unlock(LockToken token) {
        if (token == null) {
            return;
        }
        redis.execute(UNLOCK_SCRIPT, List.of(token.key()), token.value());
    }
}
