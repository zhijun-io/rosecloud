package io.rosecloud.starter.lock;

/**
 * Handle returned by a successful {@link DistributedLock#tryLock}. Carries the
 * lock key and an ownership value (a UUID for the Redis backend) needed to
 * release safely. Pass back to {@link DistributedLock#unlock}.
 */
public record LockToken(String key, String value) {
}
