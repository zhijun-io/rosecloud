package io.rosecloud.starter.cache;

import java.time.Duration;

/**
 * String-oriented cache abstraction. Keys and values are {@link String}s so the
 * starter stays free of serialization (e.g. Jackson) coupling: callers serialize
 * objects themselves (the {@code ObjectMapper} from starter-web is suitable).
 * The local default suits single-instance deployments; the Redis backend
 * provides cross-instance caching. A {@code null} ttl means the entry does not
 * expire (Redis: no expiry set; local: held until evicted).
 */
public interface RoseCloudCache {

    String get(String key);

    void put(String key, String value, Duration ttl);

    default void put(String key, String value) {
        put(key, value, null);
    }

    void evict(String key);

    boolean exists(String key);

    /**
     * Atomically increments the numeric value stored at {@code key} (treating a
     * missing key as 0) and returns the new value. The entry expires after
     * {@code ttl}, which is refreshed on every increment. A {@code null} ttl
     * means the entry never expires. Backed by Redis {@code INCR} when the Redis
     * backend is active; the local backend uses a synchronized read-modify-write.
     * Used for login-failure counters.
     */
    long increment(String key, Duration ttl);
}
