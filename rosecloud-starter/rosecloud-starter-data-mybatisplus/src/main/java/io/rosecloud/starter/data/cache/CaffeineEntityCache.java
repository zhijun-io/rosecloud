package io.rosecloud.starter.data.cache;

import com.github.benmanes.caffeine.cache.Weigher;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class CaffeineEntityCache<K extends Serializable, V>
        implements EntityCache<K, V> {

    private final String cacheName;
    private final Cache<K, V> cache;
    private final Lock lock = new ReentrantLock();
    private final Map<K, Set<UUID>> objectTransactions = new ConcurrentHashMap<>();
    private final Map<UUID, CaffeineCacheTransaction<K, V>> transactions = new ConcurrentHashMap<>();

    private static final CacheSpecs DEFAULT_SPECS = new CacheSpecs(5, 1000);

    /**
     * Creates a cache with default specs (5 min TTL, 1000 max size).
     */
    public CaffeineEntityCache(String cacheName) {
        this(cacheName, DEFAULT_SPECS);
    }

    /**
     * Creates a cache with the given {@link CacheSpecs}.
     */
    public CaffeineEntityCache(String cacheName, CacheSpecs specs) {
        this(cacheName, toBuilder(specs));
    }

    public CaffeineEntityCache(String cacheName, Caffeine<Object, Object> builder) {
        this.cacheName = Objects.requireNonNull(cacheName, "cacheName must not be null");
        this.cache = builder.build();
    }

    private static Caffeine<Object, Object> toBuilder(CacheSpecs specs) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumWeight(specs.getMaxSize())
                .weigher(collectionSafeWeigher())
                .recordStats();
        if (specs.getTimeToLiveInMinutes() > 0) {
            builder.expireAfterWrite(Duration.ofMinutes(specs.getTimeToLiveInMinutes()));
        }
        return builder;
    }

    /**
     * Returns a weigher that counts collection-size for {@link Collection} values
     * and 1 for everything else. Mirrors ThingsBoard's approach in
     * {@code TbCaffeineCacheConfiguration.collectionSafeWeigher()}.
     *
     * <p>Without this weigher, a single cache entry holding a {@code List&lt;Menu&gt;}
     * of 5000 menus counts as 1 entry toward {@code maximumSize}. With the weigher,
     * it counts as 5000 — giving more precise memory accounting for caches that
     * store aggregated collections ({@code menu.list}, {@code dept.list}, etc.).
     */
    private static Weigher<Object, Object> collectionSafeWeigher() {
        return (Weigher<Object, Object>) (key, value) -> {
            if (value instanceof Collection<?> col) {
                return col.size();
            }
            return 1;
        };
    }

    @Override public String cacheName() { return cacheName; }

    @Override @Nullable
    public V get(K key) { return cache.getIfPresent(key); }

    @Override
    public void put(K key, V value) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            cache.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void putIfAbsent(K key, V value) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            cache.asMap().putIfAbsent(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void evict(Object key) {
        lock.lock();
        try {
            failAllTransactionsByKey((K) key);
            cache.invalidate((K) key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void evictAll() {
        lock.lock();
        try {
            // Fail all pending cache transactions before wiping entries.
            for (CaffeineCacheTransaction<K, V> tr : transactions.values()) {
                tr.setFailed(true);
            }
            cache.invalidateAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V getOrLoad(K key, Supplier<V> loader, boolean cacheNullValue) {
        if (cacheNullValue) {
            return EntityCache.super.getOrLoad(key, loader, true);
        }
        // Caffeine coalesces concurrent loads for the same key (single-flight).
        return cache.get(key, k -> loader.get());
    }

    @Override
    public CacheTransaction<K, V> beginTransaction(List<K> keys) {
        lock.lock();
        try {
            var transaction = new CaffeineCacheTransaction<>(this, keys);
            var transactionId = transaction.id();
            for (K key : keys) {
                objectTransactions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(transactionId);
            }
            transactions.put(transactionId, transaction);
            return transaction;
        } finally {
            lock.unlock();
        }
    }

    boolean commit(UUID trId, Map<K, V> pendingPuts) {
        lock.lock();
        try {
            var tr = transactions.get(trId);
            if (tr == null) return false;
            var success = !tr.isFailed();
            if (success) {
                for (K key : tr.keys()) {
                    Set<UUID> others = objectTransactions.get(key);
                    if (others != null) {
                        for (UUID otherTrId : others) {
                            if (!trId.equals(otherTrId)) {
                                var other = transactions.get(otherTrId);
                                if (other != null) other.setFailed(true);
                            }
                        }
                    }
                }
                for (Map.Entry<K, V> entry : pendingPuts.entrySet()) {
                    cache.put(entry.getKey(), entry.getValue());
                }
            }
            removeTransaction(trId);
            return success;
        } finally { lock.unlock(); }
    }

    void rollback(UUID id) {
        lock.lock();
        try { removeTransaction(id); }
        finally { lock.unlock(); }
    }

    private void removeTransaction(UUID id) {
        var transaction = transactions.remove(id);
        if (transaction != null) {
            for (var key : transaction.keys()) {
                var keyTransactions = objectTransactions.get(key);
                if (keyTransactions != null) {
                    keyTransactions.remove(id);
                    if (keyTransactions.isEmpty()) objectTransactions.remove(key);
                }
            }
        }
    }

    private void failAllTransactionsByKey(K key) {
        var transactionIds = objectTransactions.get(key);
        if (transactionIds != null) {
            for (UUID trId : transactionIds) {
                var tr = transactions.get(trId);
                if (tr != null) tr.setFailed(true);
            }
        }
    }

    @Override
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() { return cache.stats(); }
}
