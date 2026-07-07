package io.rosecloud.starter.sequence;

/**
 * Monotonic sequence generator. {@code next(key)} returns the next positive long
 * for the given business key, starting at 1. The local default is per-instance
 * (resets on restart); the Redis backend is cross-instance and persists. Callers
 * namespace keys (e.g. {@code "order:2026"}) as needed.
 */
public interface SequenceGenerator {

    long next(String key);
}
