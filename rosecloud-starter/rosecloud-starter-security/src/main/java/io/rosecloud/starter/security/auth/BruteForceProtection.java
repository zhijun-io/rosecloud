package io.rosecloud.starter.security.auth;

import io.rosecloud.starter.security.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.LockedException;

import java.time.Duration;

/**
 * Account-level brute-force protection for the login and refresh endpoints.
 *
 * <p>Counts consecutive failed attempts per account in Redis; once the threshold is reached
 * the account is locked for {@link SecurityProperties.BruteForce#getLockoutDurationSeconds()}.
 * A successful authentication resets the counters. Without a Redis connection the protection
 * degrades to a no-op (with a warning) rather than breaking authentication.
 */
public class BruteForceProtection {

    private static final Logger log = LoggerFactory.getLogger(BruteForceProtection.class);
    private static final String FAIL_PREFIX = "auth:fail:";
    private static final String LOCK_PREFIX = "auth:lock:";

    private final SecurityProperties.BruteForce config;
    private final StringRedisTemplate redis;
    private final boolean enabled;

    public BruteForceProtection(SecurityProperties.BruteForce config, @Nullable StringRedisTemplate redis) {
        this.config = config;
        this.redis = redis;
        this.enabled = config.isEnabled() && redis != null;
        if (config.isEnabled() && redis == null) {
            log.warn("Brute-force protection is enabled but no StringRedisTemplate is available — "
                    + "account lockout is DISABLED. Deploy a shared Redis to enable it.");
        }
        if (enabled) {
            log.info("Brute-force protection enabled: max {} failures, {}s lockout.",
                    config.getMaxFailedAttempts(), config.getLockoutDurationSeconds());
        }
    }

    /** Throws {@link LockedException} if the account is currently locked. */
    public void assertNotLocked(String username) {
        if (!enabled || username == null) {
            return;
        }
        if (Boolean.TRUE.equals(redis.hasKey(lockKey(username)))) {
            throw new LockedException("账号已临时锁定，请稍后再试");
        }
    }

    /** Records a failed attempt and locks the account if the threshold is reached. */
    public void onFailure(String username) {
        if (!enabled || username == null) {
            return;
        }
        String failKey = failKey(username);
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, Duration.ofSeconds(config.getLockoutDurationSeconds()));
        }
        if (count != null && count >= config.getMaxFailedAttempts()) {
            redis.opsForValue().set(lockKey(username), "1",
                    Duration.ofSeconds(config.getLockoutDurationSeconds()));
            redis.delete(failKey);
            log.warn("Account '{}' locked for {}s after {} failed attempts.",
                    username, config.getLockoutDurationSeconds(), config.getMaxFailedAttempts());
        }
    }

    /** Clears any failure/lock state for the account after a successful authentication. */
    public void onSuccess(String username) {
        if (!enabled || username == null) {
            return;
        }
        redis.delete(failKey(username));
        redis.delete(lockKey(username));
    }

    private String failKey(String username) {
        return FAIL_PREFIX + username;
    }

    private String lockKey(String username) {
        return LOCK_PREFIX + username;
    }
}
