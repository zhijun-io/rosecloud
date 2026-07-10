package io.rosecloud.starter.security.auth;

import io.rosecloud.starter.security.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.LockedException;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BruteForceProtectionTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private final Set<String> locked = new HashSet<>();
    private final ConcurrentMap<String, Long> failures = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(redis.hasKey(anyString())).thenAnswer(inv -> locked.contains(inv.getArgument(0)));
        when(ops.increment(anyString())).thenAnswer(inv ->
                failures.merge(inv.getArgument(0), 1L, Long::sum));
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);
        doAnswer(inv -> {
            locked.add(inv.getArgument(0));
            return null;
        }).when(ops).set(anyString(), anyString(), any(Duration.class));
        doAnswer(inv -> {
            locked.remove(inv.getArgument(0));
            failures.remove(inv.getArgument(0));
            return null;
        }).when(redis).delete(anyString());
    }

    @Test
    void locksAccountAfterMaxFailures() {
        SecurityProperties.BruteForce config = new SecurityProperties.BruteForce();
        config.setMaxFailedAttempts(3);
        config.setLockoutDurationSeconds(900);
        BruteForceProtection protection = new BruteForceProtection(config, redis);

        protection.assertNotLocked("alice");
        protection.onFailure("alice");
        protection.onFailure("alice");
        assertDoesNotThrow(() -> protection.assertNotLocked("alice")); // 2 < 3, still allowed
        protection.onFailure("alice"); // 3rd failure -> locked

        assertThrows(LockedException.class, () -> protection.assertNotLocked("alice"));
    }

    @Test
    void onSuccessClearsLockState() {
        SecurityProperties.BruteForce config = new SecurityProperties.BruteForce();
        config.setMaxFailedAttempts(1);
        config.setLockoutDurationSeconds(900);
        BruteForceProtection protection = new BruteForceProtection(config, redis);

        protection.onFailure("bob"); // 1 failure -> locked immediately
        assertThrows(LockedException.class, () -> protection.assertNotLocked("bob"));

        protection.onSuccess("bob"); // success resets state
        assertDoesNotThrow(() -> protection.assertNotLocked("bob"));
    }

    @Test
    void disabledProtectionNeverLocks() {
        SecurityProperties.BruteForce config = new SecurityProperties.BruteForce();
        config.setEnabled(false);
        BruteForceProtection protection = new BruteForceProtection(config, redis);

        for (int i = 0; i < 100; i++) {
            protection.onFailure("carol");
        }
        assertDoesNotThrow(() -> protection.assertNotLocked("carol"));
    }
}
