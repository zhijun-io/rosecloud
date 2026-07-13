package io.rosecloud.starter.data.event;

import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.data.cache.EntityCache;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link CacheEvictionListener}.
 */
class CacheEvictionListenerTest {

    @Test
    void shouldEvictCacheOnEntityChangedEvent() {
        @SuppressWarnings("unchecked")
        EntityCache<String, String> cache = mock(EntityCache.class);
        when(cache.cacheName()).thenReturn("user.security");

        CacheEvictionListener listener = new CacheEvictionListener();
        listener.register(cache);

        listener.onEntityChanged(
                EntityChangedEvent.deleted("user.security", "admin@example.com", "ROOT", null));

        verify(cache).evict("admin@example.com");
    }

    @Test
    void shouldNotEvictUnregisteredCacheType() {
        @SuppressWarnings("unchecked")
        EntityCache<String, String> cache = mock(EntityCache.class);
        when(cache.cacheName()).thenReturn("user.security");

        CacheEvictionListener listener = new CacheEvictionListener();
        listener.register(cache);

        listener.onEntityChanged(
                EntityChangedEvent.created("unknown.type", "abc", "ROOT", null));

        verify(cache, never()).evict(any());
    }

    @Test
    void shouldRegisterAllCaches() {
        @SuppressWarnings("unchecked")
        EntityCache<String, String> cache1 = mock(EntityCache.class);
        when(cache1.cacheName()).thenReturn("cache1");
        @SuppressWarnings("unchecked")
        EntityCache<String, String> cache2 = mock(EntityCache.class);
        when(cache2.cacheName()).thenReturn("cache2");

        CacheEvictionListener listener = new CacheEvictionListener();
        listener.registerAll(List.of(cache1, cache2));

        listener.onEntityChanged(
                EntityChangedEvent.deleted("cache1", "x", null, null));
        verify(cache1).evict("x");
        verify(cache2, never()).evict(any());
    }
}
