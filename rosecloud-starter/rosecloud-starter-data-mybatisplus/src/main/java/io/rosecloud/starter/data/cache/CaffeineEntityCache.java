package io.rosecloud.starter.data.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * 基于 Caffeine 的实体缓存实现。
 *
 * <p>默认配置：最大条目 1000，写入后 5 分钟过期。
 * 通过 {@link #builder()} 可自定义缓存策略。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public class CaffeineEntityCache<K, V> implements EntityCache<K, V> {

    private final String cacheName;
    private final Cache<K, V> cache;

    public CaffeineEntityCache(String cacheName) {
        this(cacheName, Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
    }

    public CaffeineEntityCache(String cacheName, Caffeine<Object, Object> builder) {
        this.cacheName = Objects.requireNonNull(cacheName, "cacheName must not be null");
        this.cache = builder.build();
    }

    @Override
    public String cacheName() {
        return cacheName;
    }

    @Override
    @Nullable
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void evict(Object key) {
        cache.invalidate((K) key);
    }

    @Override
    public void evictAll() {
        cache.invalidateAll();
    }

    /** 返回 Caffeine 统计信息，用于监控。 */
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return cache.stats();
    }
}
