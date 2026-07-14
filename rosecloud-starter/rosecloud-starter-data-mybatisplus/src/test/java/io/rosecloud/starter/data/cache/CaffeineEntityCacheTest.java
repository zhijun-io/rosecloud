package io.rosecloud.starter.data.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaffeineEntityCacheTest {

    @Test
    void getOrLoadTransactionalReturnsCachedValueWithoutLoading() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        cache.put("key", "cached");
        String result = cache.getOrLoadTransactional("key", () -> "loaded");
        assertEquals("cached", result);
    }

    @Test
    void getOrLoadTransactionalLoadsAndCachesWhenNoTransaction() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        String result = cache.getOrLoadTransactional("key", () -> "loaded");
        assertEquals("loaded", result);
        assertEquals("loaded", cache.get("key"));
    }

    @Test
    void getOrLoadTransactionalReturnsNullWithoutCachingWhenLoaderReturnsNull() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        String result = cache.getOrLoadTransactional("key", () -> null);
        assertNull(result);
        assertNull(cache.get("key"));
    }

    @Test
    void getReturnsNullForMissingKey() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        assertNull(cache.get("missing"));
    }

    @Test
    void putAndGet() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    void evictRemovesKey() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        cache.put("key1", "value1");
        cache.evict("key1");
        assertNull(cache.get("key1"));
    }

    @Test
    void evictAllClearsCache() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.evictAll();
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void getOrLoadLoadsMissing() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        String result = cache.getOrLoad("key1", () -> "loaded");
        assertEquals("loaded", result);
        assertEquals("loaded", cache.get("key1"));
    }

    @Test
    void getOrLoadReturnsExistingWithoutLoading() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        cache.put("key1", "cached");
        String result = cache.getOrLoad("key1", () -> "loaded");
        assertEquals("cached", result);
    }

    @Test
    void statsRecordsHitsAndMisses() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        cache.put("key1", "value1");
        cache.get("key1"); // hit
        cache.get("key2"); // miss
        var stats = cache.stats();
        assertEquals(1, stats.hitCount());
        assertEquals(1, stats.missCount());
    }

    @Test
    void putIfAbsentDoesNotOverwriteExisting() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        cache.put("key", "first");
        cache.putIfAbsent("key", "second");
        assertEquals("first", cache.get("key"));
    }

    @Test
    void putIfAbsentStoresWhenMissing() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        cache.putIfAbsent("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    void beginTransactionCommitAppliesPendingPuts() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        var tx = cache.beginTransaction("key");
        tx.put("key", "pending");
        assertNull(cache.get("key"));
        assertTrue(tx.commit());
        assertEquals("pending", cache.get("key"));
    }

    @Test
    void evictFailsPendingTransaction() {
        CaffeineEntityCache<String, String> cache = new CaffeineEntityCache<>("test");
        var tx = cache.beginTransaction("key");
        tx.put("key", "pending");
        cache.evict("key");
        assertFalse(tx.commit());
        assertNull(cache.get("key"));
    }
}
