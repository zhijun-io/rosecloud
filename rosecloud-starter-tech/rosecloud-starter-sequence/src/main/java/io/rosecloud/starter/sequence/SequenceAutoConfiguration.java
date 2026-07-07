package io.rosecloud.starter.sequence;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Registers a {@link SequenceGenerator}. Activated by
 * {@code rosecloud.sequence.enabled=true}; the Redis backend is used when
 * {@code rosecloud.sequence.type=redis} and a Redis client is present, otherwise
 * the in-memory default (single-instance) applies. Requires the consumer to add
 * {@code spring-boot-starter-data-redis} for the Redis backend.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.sequence", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SequenceProperties.class)
public class SequenceAutoConfiguration {

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "rosecloud.sequence", name = "type", havingValue = "redis")
    @ConditionalOnMissingBean
    public SequenceGenerator redisSequenceGenerator(StringRedisTemplate redisTemplate) {
        return new RedisSequenceGenerator(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public SequenceGenerator localSequenceGenerator() {
        return new LocalSequenceGenerator();
    }
}
