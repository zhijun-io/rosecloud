package io.rosecloud.starter.data.event;

import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.data.cache.EntityCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 缓存失效监听器。
 *
 * <p>监听 {@link EntityChangedEvent}，根据是否在活跃事务中决定失效时机：
 * <ul>
 *   <li>有事务时：通过 {@code @TransactionalEventListener(AFTER_COMMIT)} 在事务提交后失效</li>
 *   <li>无事务时：通过 {@link #evictNow(EntityChangedEvent)} 立即失效</li>
 * </ul>
 * 该模式借鉴 ThingsBoard {@code AbstractCachedEntityService.publishEvictEvent()}。
 *
 * <p>各模块通过 {@link #register(String, EntityCache)} 注册需要失效的缓存实例。
 */
public class CacheEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionListener.class);

    private final ConcurrentMap<String, EntityCache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * 注册一个缓存实例，其 {@code cacheName} 将作为 entityType 匹配依据。
     */
    public void register(EntityCache<?, ?> cache) {
        caches.put(cache.cacheName(), cache);
    }

    /**
     * 批量注册。
     */
    public void registerAll(List<EntityCache<?, ?>> caches) {
        for (EntityCache<?, ?> cache : caches) {
            register(cache);
        }
    }

    /**
     * 在事务提交后执行缓存失效。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntityChanged(EntityChangedEvent<?> event) {
        evictNow(event);
    }

    /**
     * 立即执行缓存失效，不等待事务提交。
     * <p>被 {@link EntityEventPublisher} 在非事务上下文中调用，
     * 与 ThingsBoard {@code AbstractCachedEntityService.handleEvictEvent()} 语义一致。
     */
    public void evictNow(EntityChangedEvent<?> event) {
        evictCache(event);
    }

    /**
     * 返回所有注册缓存的实时统计信息。
     * <p>借鉴 ThingsBoard {@code TbCaffeineCacheConfiguration} 对 Cache 实例调用
     * {@code recordStats()} 后通过自定义端点暴露的监控模式。
     * 仅对 {@link io.rosecloud.starter.data.cache.CaffeineEntityCache} 生效，
     * 其他实现返回 {@code null} 的 stats 会被跳过。
     */
    public Map<String, com.github.benmanes.caffeine.cache.stats.CacheStats> cacheStats() {
        Map<String, com.github.benmanes.caffeine.cache.stats.CacheStats> stats = new java.util.LinkedHashMap<>();
        caches.forEach((name, cache) -> {
            var s = cache.stats();
            if (s != null) {
                stats.put(name, s);
            }
        });
        return stats;
    }

    private void evictCache(EntityChangedEvent<?> event) {
        EntityCache<?, ?> cache = caches.get(event.entityType());
        if (cache != null) {
            log.debug("Evicting cache [{}] for key [{}]", event.entityType(), event.entityId());
            cache.evict(event.entityId());
        }
    }
}
