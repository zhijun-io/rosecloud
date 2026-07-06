package io.rosecloud.starter.lock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registers a {@link DistributedLock}. Activated by
 * {@code rosecloud.lock.enabled=true}; the Redis backend is used when
 * {@code rosecloud.lock.type=redis} and a Redis client is present, otherwise
 * the in-memory default (single-instance) applies. Requires the consumer to add
 * {@code spring-boot-starter-data-redis} for the Redis backend.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.lock", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(LockProperties.class)
public class LockAutoConfiguration {

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "rosecloud.lock", name = "type", havingValue = "redis")
    @ConditionalOnMissingBean
    public DistributedLock redisDistributedLock(StringRedisTemplate redisTemplate) {
        return new RedisDistributedLock(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLock localDistributedLock() {
        return new LocalDistributedLock();
    }
}
