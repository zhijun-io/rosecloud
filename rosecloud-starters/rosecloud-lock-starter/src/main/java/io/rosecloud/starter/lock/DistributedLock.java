package io.rosecloud.starter.lock;

import java.time.Duration;

/**
 * Distributed lock abstraction. {@code tryLock} is non-blocking: it returns a
 * {@link LockToken} when the lock is acquired, or {@code null} otherwise. The
 * local default suits single-instance deployments; the Redis backend provides
 * cross-instance mutual exclusion. Callers must {@link #unlock} in a finally
 * block; the {@code ttl} bounds a Redis lock so a crashed holder cannot hold it
 * forever (ignored by the local implementation).
 */
public interface DistributedLock {

    LockToken tryLock(String key, Duration ttl);

    void unlock(LockToken token);
}
