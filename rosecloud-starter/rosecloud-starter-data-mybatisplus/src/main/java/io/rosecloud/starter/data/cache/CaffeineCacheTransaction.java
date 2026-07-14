package io.rosecloud.starter.data.cache;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CaffeineCacheTransaction<K extends Serializable, V>
        implements CacheTransaction<K, V> {
    private final UUID id = UUID.randomUUID();
    private final CaffeineEntityCache<K, V> cache;
    private final List<K> keys;
    private boolean failed;
    private final Map<K, V> pendingPuts = new LinkedHashMap<>();
    public CaffeineCacheTransaction(CaffeineEntityCache<K, V> cache, List<K> keys) { this.cache = cache; this.keys = keys; }
    UUID id() { return id; }
    List<K> keys() { return keys; }
    boolean isFailed() { return failed; }
    void setFailed(boolean failed) { this.failed = failed; }
    @Override public void put(K key, V value) { pendingPuts.put(key, value); }
    @Override public boolean commit() { return cache.commit(id, pendingPuts); }
    @Override public void rollback() { cache.rollback(id); }
}
