package io.rosecloud.starter.cache;

import org.springframework.beans.factory.DisposableBean;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory {@link RoseCloudCache} for single-instance (e.g. monolith)
 * deployments. Entries are held in a {@link ConcurrentHashMap}; expired entries
 * are evicted lazily on read and periodically by a background sweep so the map
 * cannot grow without bound from keys that are written but never read again.
 * {@code ttl=null} stores a non-expiring entry. Not suitable for multi-instance
 * consistency—use the Redis backend.
 */
public class LocalRoseCloudCache implements RoseCloudCache, DisposableBean {

    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();
    private final Clock clock;
    private final ScheduledExecutorService sweeper;

    public LocalRoseCloudCache() {
        this(Clock.systemUTC());
    }

    LocalRoseCloudCache(Clock clock) {
        this.clock = clock;
        this.sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "local-rosecloud-cache-sweeper");
            t.setDaemon(true);
            return t;
        });
        this.sweeper.scheduleAtFixedRate(this::sweepExpired, 60, 60, TimeUnit.SECONDS);
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

    @Override
    public synchronized long increment(String key, Duration ttl) {
        CacheEntry entry = store.get(key);
        long current = 0;
        if (entry != null && !entry.isExpired(clock.millis())) {
            try {
                current = Long.parseLong(entry.value());
            } catch (NumberFormatException ignored) {
                current = 0;
            }
        }
        long next = current + 1;
        long expireAt = ttl == null ? -1 : clock.millis() + ttl.toMillis();
        store.put(key, new CacheEntry(Long.toString(next), expireAt));
        return next;
    }

    private void sweepExpired() {
        long now = clock.millis();
        store.values().removeIf(entry -> entry.isExpired(now));
    }

    @Override
    public void destroy() {
        sweeper.shutdownNow();
    }
}
