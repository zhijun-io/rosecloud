package io.rosecloud.starter.cache;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link RoseCloudCache} for single-instance (e.g. monolith)
 * deployments. Entries are held in a {@link ConcurrentHashMap}; expired entries
 * are evicted lazily on read. {@code ttl=null} stores a non-expiring entry. Not
 * suitable for multi-instance consistency—use the Redis backend.
 */
public class LocalRoseCloudCache implements RoseCloudCache {

    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();
    private final Clock clock;

    public LocalRoseCloudCache() {
        this(Clock.systemUTC());
    }

    LocalRoseCloudCache(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String get(String key) {
        CacheEntry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(clock.millis())) {
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        long expireAt = ttl == null ? -1 : clock.millis() + ttl.toMillis();
        store.put(key, new CacheEntry(value, expireAt));
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }
}
