package io.rosecloud.system.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.rosecloud.starter.data.event.CacheEvictionListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 缓存统计信息端点。
 *
 * <p>借鉴 ThingsBoard {@code TbCaffeineCacheConfiguration} 通过自定义端点暴露 Caffeine
 * 缓存命中率、请求数、驱逐数等指标的模式，提供 RoseCloud Caffeine 缓存的运行态可见性。
 *
 * <p>所有 {@link io.rosecloud.starter.data.cache.CaffeineEntityCache} 实例的统计
 * 由 {@link CacheStatsController#cacheStats()} 聚合返回，非 Caffeine 实现的缓存（如 Redis）
 * 会被自动跳过（其 {@code stats()} 返回 {@code null}）。
 *
 * <p>示例返回：
 * <pre>{@code
 * {
 *   "user.security": { "hitCount": 1234, "missCount": 56, "hitRate": 0.956, "evictionCount": 0, "averageLoadPenalty": 0.00234 },
 *   "menu.list":     { "hitCount": 891,  "missCount": 12, "hitRate": 0.987, "evictionCount": 1, "averageLoadPenalty": 0.00112 }
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/cache-stats")
public class CacheStatsController {

    private final CacheEvictionListener cacheEvictionListener;

    public CacheStatsController(CacheEvictionListener cacheEvictionListener) {
        this.cacheEvictionListener = cacheEvictionListener;
    }

    @GetMapping
    public Map<String, Object> getCacheStats() {
        Map<String, CacheStats> raw = cacheEvictionListener.cacheStats();
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((name, stats) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("hitCount", stats.hitCount());
            entry.put("missCount", stats.missCount());
            entry.put("hitRate", stats.hitRate());
            entry.put("evictionCount", stats.evictionCount());
            entry.put("averageLoadPenalty", stats.averageLoadPenalty());
            result.put(name, entry);
        });
        return result;
    }
}
