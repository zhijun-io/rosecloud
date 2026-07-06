package io.rosecloud.starter.cache;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link LocalRoseCloudCache}: put/get/evict, lazy eviction of expired
 * entries, and non-expiring entries. A {@link MutableClock} makes ttl expiry
 * deterministic (no thread sleeps).
 */
class LocalRoseCloudCacheTest {

    @Test
    void putGetEvictAndExists() {
        LocalRoseCloudCache cache = new LocalRoseCloudCache();

        assertThat(cache.get("missing")).isNull();
        assertThat(cache.exists("missing")).isFalse();

        cache.put("user:1", "alice");
        assertThat(cache.get("user:1")).isEqualTo("alice");
        assertThat(cache.exists("user:1")).isTrue();

        cache.evict("user:1");
        assertThat(cache.get("user:1")).isNull();
        assertThat(cache.exists("user:1")).isFalse();
    }

    @Test
    void expiredEntryIsEvictedLazily() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-07T00:00:00Z"));
        LocalRoseCloudCache cache = new LocalRoseCloudCache(clock);

        cache.put("token:1", "abc", Duration.ofSeconds(30));
        assertThat(cache.get("token:1")).isEqualTo("abc");

        // Advance just past the ttl: the entry is expired and cleared on read.
        clock.advance(Duration.ofSeconds(31));
        assertThat(cache.get("token:1")).isNull();
        assertThat(cache.exists("token:1")).isFalse();
    }

    @Test
    void noTtlEntryNeverExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-07T00:00:00Z"));
        LocalRoseCloudCache cache = new LocalRoseCloudCache(clock);

        cache.put("config:1", "value"); // no ttl
        clock.advance(Duration.ofSeconds(10_000));
        assertThat(cache.get("config:1")).isEqualTo("value");
    }

    /** A {@link Clock} whose instant can be advanced in tests. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            this.now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
