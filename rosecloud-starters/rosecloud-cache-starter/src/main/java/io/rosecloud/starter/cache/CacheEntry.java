package io.rosecloud.starter.cache;

/** A cached value with its absolute expiry (epoch millis); {@code -1} = never. */
record CacheEntry(String value, long expireAtMillis) {

    boolean isExpired(long nowMillis) {
        return expireAtMillis != -1 && nowMillis >= expireAtMillis;
    }
}
