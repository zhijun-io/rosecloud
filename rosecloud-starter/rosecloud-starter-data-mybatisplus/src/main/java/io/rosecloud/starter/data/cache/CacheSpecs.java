package io.rosecloud.starter.data.cache;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Cache configuration specs — TTL and maxSize — mirroring ThingsBoard {@code CacheSpecs}.
 *
 * <p>Each named cache gets its own spec, loaded from {@code application.yml} under
 * the {@code cache.specs} prefix via {@link CachingConfigProperties}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheSpecs {

    /** Time-to-live in minutes. Zero means no expiration. */
    private int timeToLiveInMinutes = 5;

    /** Maximum number of entries in the cache. Zero means the cache is disabled. */
    private int maxSize = 1000;

}
