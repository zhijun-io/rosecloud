package io.rosecloud.starter.sequence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link SequenceGenerator} for single-instance (e.g. monolith)
 * deployments. A per-key {@link AtomicLong} starts at 0 and returns 1 on first
 * {@code next}. Counters are per-instance and reset on restart—use the Redis
 * backend for cross-instance persistence.
 */
public class LocalSequenceGenerator implements SequenceGenerator {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public long next(String key) {
        return counters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }
}
