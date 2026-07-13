package io.rosecloud.system.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.rosecloud.starter.data.cache.CaffeineEntityCache;
import io.rosecloud.starter.data.cache.EntityCache;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 缓存统计监控端点。返回所有 Caffeine 缓存的命中率、请求数、驱逐数等统计信息。
 *
 * <p>参考 ThingsBoard {@code TbCaffeineCacheConfiguration} 的 {@code recordStats()} 模式，
 * RoseCloud 的 {@link CaffeineEntityCache} 默认启用了统计。
 * 本端点提供 Actuator 之外的轻量级视图。
 */
@RestController
@RequestMapping("/actuator/cache-stats")
@RequiredArgsConstructor
public class CacheStatsController {

    private final ApplicationContext applicationContext;

    @GetMapping
    public Map<String, Object> stats() {
        Map<String, EntityCache> beans = applicationContext.getBeansOfType(EntityCache.class);
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, EntityCache> entry : beans.entrySet()) {
            if (entry.getValue() instanceof CaffeineEntityCache caffeineCache) {
                CacheStats stats = caffeineCache.stats();
                Map<String, Object> cacheStats = new LinkedHashMap<>();
                cacheStats.put("hitCount", stats.hitCount());
                cacheStats.put("missCount", stats.missCount());
                cacheStats.put("hitRate", stats.hitRate());
                cacheStats.put("missRate", stats.missRate());
                cacheStats.put("requestCount", stats.requestCount());
                cacheStats.put("evictionCount", stats.evictionCount());
                cacheStats.put("evictionWeight", stats.evictionWeight());
                cacheStats.put("averageLoadPenalty", stats.averageLoadPenalty());
                result.put(caffeineCache.cacheName(), cacheStats);
            }
        }
        return result;
    }
}
