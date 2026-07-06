package io.rosecloud.starter.lock;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory {@link DistributedLock} for single-instance (e.g. monolith)
 * deployments. Non-blocking {@link ReentrantLock} per key; {@code ttl} is
 * ignored (a local holder is released only by explicit unlock). Not suitable
 * for multi-instance mutual exclusion—use the Redis backend.
 */
public class LocalDistributedLock implements DistributedLock {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public LockToken tryLock(String key, Duration ttl) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        return lock.tryLock() ? new LockToken(key, "local") : null;
    }

    @Override
    public void unlock(LockToken token) {
        if (token == null) {
            return;
        }
        ReentrantLock lock = locks.get(token.key());
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
