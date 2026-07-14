package io.rosecloud.starter.data.cache;

import com.github.benmanes.caffeine.cache.Weigher;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Spring Cache integration layer using Caffeine backed by the same {@link CacheSpecs}
 * configuration as {@link EntityCache}.
 *
 * <p>This allows services to optionally use {@code @Cacheable} / {@code @CacheEvict}
 * for simple caching needs, while the custom {@link EntityCache} remains the primary
 * abstraction for transaction-aware caching (put-after-commit, null-value control,
 * per-entry eviction events).
 *
 * <p>Cache specs are shared: the same {@code cache.specs.*} entries in
 * {@code application.yml} configure both the Spring {@link CacheManager} and
 * the explicit {@link CaffeineEntityCache} beans.
 *
 * <p>ThingsBoard's TbCaffeineCacheConfiguration uses the same pattern —
 * building a Spring CacheManager from externally-configured CacheSpecs,
 * then using {@code @Cacheable} in DAO services.
 *
 * @see CachingConfigProperties
 * @see CaffeineEntityCache
 */
@AutoConfiguration
@EnableCaching
@ConditionalOnProperty(prefix = "cache", name = "spring-cache-enabled", havingValue = "true", matchIfMissing = true)
public class CaffeineCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(CachingConfigProperties cachingConfigProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);
        if (cachingConfigProperties.getSpecs() != null) {
            cachingConfigProperties.getSpecs().forEach((name, spec) -> {
                Caffeine<Object, Object> builder = Caffeine.newBuilder()
                        .maximumWeight(spec.getMaxSize())
                        .weigher(collectionSafeWeigher())
                        .recordStats();
                if (spec.getTimeToLiveInMinutes() > 0) {
                    builder.expireAfterWrite(spec.getTimeToLiveInMinutes(), TimeUnit.MINUTES);
                }
                manager.registerCustomCache(name, builder.build());
            });
        }
        return manager;
    }

    /**
     * Collection-safe weigher: counts Collection values by their size, others as 1.
     * Mirrors ThingsBoard's {@code TbCaffeineCacheConfiguration.collectionSafeWeigher()}.
     */
    private static Weigher<Object, Object> collectionSafeWeigher() {
        return (Weigher<Object, Object>) (key, value) -> {
            if (value instanceof Collection<?> col) {
                return col.size();
            }
            return 1;
        };
    }

}
