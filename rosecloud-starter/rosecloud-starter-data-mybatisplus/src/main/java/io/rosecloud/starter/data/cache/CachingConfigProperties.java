package io.rosecloud.starter.data.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Maps cache names to their {@link CacheSpecs}, mirroring ThingsBoard {@code CacheSpecsMap}.
 *
 * <p>Configured via {@code application.yml}:
 * <pre>{@code
 * cache:
 *   specs:
 *     tenant:
 *       timeToLiveInMinutes: 30
 *       maxSize: 5000
 *     menu:
 *       timeToLiveInMinutes: 60
 *       maxSize: 10000
 * }</pre>
 *
 * Any cache not explicitly listed falls back to a default spec (5 min TTL, 1000 max size),
 * set inside {@link CaffeineEntityCache}.
 */
@ConfigurationProperties(prefix = "cache")
@Data
public class CachingConfigProperties {

    private Map<String, CacheSpecs> specs;

}
