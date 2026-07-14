package io.rosecloud.starter.data.cache;

import jakarta.annotation.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

public interface EntityCache<K extends Serializable, V> {
    String cacheName();
    @Nullable
    V get(K key);
    void put(K key, V value);
    void putIfAbsent(K key, V value);
    void evict(Object key);
    default void evictOrPut(K key, V value) { evict(key); }
    void evictAll();
    default CacheTransaction<K, V> beginTransaction(K key) { return beginTransaction(List.of(key)); }
    CacheTransaction<K, V> beginTransaction(List<K> keys);
    /**
     * Load on miss and publish into cache only after the surrounding Spring
     * transaction commits, using {@link #beginTransaction(Object)} so concurrent
     * writers can invalidate the pending put via {@link CacheTransaction}.
     */
    default V getOrLoadTransactional(K key, Supplier<V> loader) {
        V v = get(key);
        if (v != null) {
            return v;
        }
        V loaded = loader.get();
        if (loaded == null) {
            return null;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            CacheTransaction<K, V> tx = beginTransaction(key);
            tx.put(key, loaded);
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            tx.commit();
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                tx.rollback();
                            }
                        }
                    });
        } else {
            put(key, loaded);
        }
        return loaded;
    }
    default V getOrLoad(K key, Supplier<V> loader, boolean cacheNullValue) {
        V v = get(key);
        if (v != null) return v;
        V loaded = loader.get();
        if (loaded != null || cacheNullValue) put(key, loaded);
        return loaded;
    }
    default V getOrLoad(K key, Supplier<V> loader) { return getOrLoad(key, loader, false); }
    @Nullable
    default com.github.benmanes.caffeine.cache.stats.CacheStats stats() { return null; }
}
