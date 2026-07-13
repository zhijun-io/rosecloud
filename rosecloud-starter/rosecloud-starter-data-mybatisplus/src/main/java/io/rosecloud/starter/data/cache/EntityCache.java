package io.rosecloud.starter.data.cache;

import jakarta.annotation.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Supplier;

/**
 * 实体缓存接口。
 *
 * <p>每个缓存实例需关联一个不变的 {@code cacheName}（见 {@link io.rosecloud.starter.data.EntityCacheNames}），
 * 供事件驱动的缓存失效监听器按名称匹配定位。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public interface EntityCache<K, V> {

    /** 缓存唯一名称，用于事件路由和监控。 */
    String cacheName();

    /** 从缓存中取值，未命中返回 {@code null}。 */
    @Nullable
    V get(K key);

    /** 写入缓存。 */
    void put(K key, V value);

    /** 移除指定键的缓存。 */
    void evict(Object key);

    /** 清空全缓存。 */
    void evictAll();

    /**
     * 事务型读穿透：优先读缓存，未命中则通过 {@code loader} 加载并回填。
     * <p>区别于 {@link #getOrLoad(Object, Supplier)}，当调用方处于活跃事务中时，
     * 缓存写入会推迟到事务提交之后（{@link TransactionSynchronization#afterCommit()}），
     * 避免事务回滚后缓存中出现脏数据。无事务时行为与 getOrLoad 一致。
     *
     * <p>借鉴 ThingsBoard {@code TbTransactionalCache.getAndPutInTransaction()}，
     * 使用 {@link TransactionSynchronizationManager#registerSynchronization(TransactionSynchronization)}
     * 实现提交后写入。
     *
     * @see #getOrLoad(Object, Supplier)
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
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            put(key, loaded);
                        }
                    }
            );
        } else {
            put(key, loaded);
        }
        return loaded;
    }

    /**
     * 读穿透：优先读缓存，未命中则通过 {@code loader} 加载并回填。
     * 如果 loader 返回 {@code null}，不回填。
    */
    default V getOrLoad(K key, Supplier<V> loader) {
        V v = get(key);
        if (v != null) {
            return v;
        }
        V loaded = loader.get();
        if (loaded != null) {
            put(key, loaded);
        }
        return loaded;
    }

    /**
     * 返回缓存统计信息。非 Caffeine 实现返回 null。
     * 用于监控和管理。
     */
    @Nullable
    default com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return null;
    }
}
