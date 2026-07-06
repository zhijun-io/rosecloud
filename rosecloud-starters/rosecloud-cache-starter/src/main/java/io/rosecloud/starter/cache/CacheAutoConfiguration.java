package io.rosecloud.starter.cache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registers a {@link RoseCloudCache}. Activated by
 * {@code rosecloud.cache.enabled=true}; the Redis backend is used when
 * {@code rosecloud.cache.type=redis} and a Redis client is present, otherwise
 * the in-memory default (single-instance) applies. Requires the consumer to add
 * {@code spring-boot-starter-data-redis} for the Redis backend.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.cache", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "rosecloud.cache", name = "type", havingValue = "redis")
    @ConditionalOnMissingBean
    public RoseCloudCache redisRoseCloudCache(StringRedisTemplate redisTemplate) {
        return new RedisRoseCloudCache(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoseCloudCache localRoseCloudCache() {
        return new LocalRoseCloudCache();
    }
}
