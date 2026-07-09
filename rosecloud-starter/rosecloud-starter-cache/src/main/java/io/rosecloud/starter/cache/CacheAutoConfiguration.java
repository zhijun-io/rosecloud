package io.rosecloud.starter.cache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registers a {@link RoseCloudCache}. Activated by
 * {@code rosecloud.cache.enabled=true}; the Redis backend is used when
 * {@code rosecloud.cache.type=redis} and a Redis client is present, otherwise
 * the in-memory default (single-instance) applies. Requires the consumer to add
 * {@code spring-boot-starter-data-redis} for the Redis backend.
 *
 * <p>The Redis bean lives in a nested configuration guarded by
 * {@link ConditionalOnClass}, so this auto-configuration can be activated even
 * when {@code StringRedisTemplate} is absent from the classpath (e.g. a
 * single-instance monolith that excludes Redis) without triggering a
 * {@link NoClassDefFoundError} during condition evaluation.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.cache", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "rosecloud.cache", name = "type", havingValue = "in-memory", matchIfMissing = true)
    @ConditionalOnMissingBean(RoseCloudCache.class)
    public RoseCloudCache localRoseCloudCache() {
        return new LocalRoseCloudCache();
    }

    @Configuration
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "rosecloud.cache", name = "type", havingValue = "redis")
    static class RedisCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(RoseCloudCache.class)
        public RoseCloudCache redisRoseCloudCache(StringRedisTemplate redisTemplate) {
            return new RedisRoseCloudCache(redisTemplate);
        }
    }
}
