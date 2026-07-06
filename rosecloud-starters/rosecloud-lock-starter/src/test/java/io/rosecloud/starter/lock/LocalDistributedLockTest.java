package io.rosecloud.starter.lock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link LocalDistributedLock} provides per-key mutual exclusion: a
 * second thread cannot acquire a held key, but can once the holder releases it.
 * A separate thread is required because {@link java.util.concurrent.locks.ReentrantLock}
 * is reentrant for the owning thread. {@code ttl} is ignored by the local
 * implementation.
 */
class LocalDistributedLockTest {

    @Test
    void enforcesMutualExclusionPerKey() throws InterruptedException {
        LocalDistributedLock lock = new LocalDistributedLock();

        LockToken first = lock.tryLock("order:1", Duration.ofSeconds(30));
        assertThat(first).isNotNull();

        // A second thread cannot acquire the held key.
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<LockToken> contender = new AtomicReference<>();
        Thread other = new Thread(() -> {
            contender.set(lock.tryLock("order:1", Duration.ofSeconds(30)));
            attempted.countDown();
        });
        other.start();
        attempted.await();
        other.join();
        assertThat(contender.get()).isNull();

        // An unrelated key is unaffected.
        LockToken otherKey = lock.tryLock("order:2", Duration.ofSeconds(30));
        assertThat(otherKey).isNotNull();

        lock.unlock(first);

        // After release the previously contended key is acquirable again.
        LockToken reacquired = lock.tryLock("order:1", Duration.ofSeconds(30));
        assertThat(reacquired).isNotNull();

        lock.unlock(otherKey);
        lock.unlock(reacquired);
    }

    @Test
    void unlockNullTokenIsNoOp() {
        LocalDistributedLock lock = new LocalDistributedLock();
        lock.unlock(null);
    }
}
