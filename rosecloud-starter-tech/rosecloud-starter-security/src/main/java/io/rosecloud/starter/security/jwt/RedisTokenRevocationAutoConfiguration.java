package io.rosecloud.starter.security.jwt;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Activates the Redis-backed {@link TokenRevocationService} when
 * {@code rosecloud.security.jwt.revocation.type=redis} and a Redis client is
 * present. Otherwise the in-memory default from {@link JwtAutoConfiguration}
 * applies. Requires the consumer to add {@code spring-boot-starter-data-redis}.
 */
@AutoConfiguration(before = JwtAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "rosecloud.security.jwt.revocation", name = "type", havingValue = "redis")
@EnableConfigurationProperties(TokenRevocationProperties.class)
public class RedisTokenRevocationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenRevocationService tokenRevocationService(StringRedisTemplate redisTemplate) {
        return new RedisTokenRevocationService(redisTemplate);
    }
}
