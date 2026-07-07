package io.rosecloud.auth.service.security;

import io.rosecloud.auth.config.LoginProtectionProperties;
import io.rosecloud.auth.error.AuthErrorCode;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.cache.RoseCloudCache;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stateful login brute-force protection. Two complementary layers:
 *
 * <ul>
 *   <li><b>Per-account lockout</b>: after {@code maxAttempts} failed password
 *       attempts within {@code lockWindow}, the account is locked for a duration
 *       that doubles on each subsequent lock event (exponential backoff), capped
 *       at {@code maxLockDuration}.</li>
 *   <li><b>Per-IP rate limit</b>: after {@code ipMaxAttempts} failures from one
 *       IP within {@code ipWindow}, that IP is temporarily blocked.</li>
 * </ul>
 *
 * All counters live in the {@link RoseCloudCache} so the policy is consistent
 * across auth-service instances (Redis backend) or a single monolith (local
 * backend). The protection is <i>fail-open</i>: if the cache is unreachable the
 * login still proceeds (and is only logged), so a cache outage cannot lock
 * everyone out.
 */
@Service
public class LoginProtectionService {

    private static final Logger log = LoggerFactory.getLogger(LoginProtectionService.class);

    private final RoseCloudCache cache;
    private final LoginProtectionProperties props;

    public LoginProtectionService(RoseCloudCache cache, LoginProtectionProperties props) {
        this.cache = cache;
        this.props = props;
    }

    /**
     * Blocks the request before any credential work when the account or IP is
     * currently limited.
     */
    public void checkAllowed(String username, String ip) {
        if (!props.isEnabled()) {
            return;
        }
        try {
            if (cache.exists(lockKey(username))) {
                throw new BizException(AuthErrorCode.ACCOUNT_LOCKED);
            }
            if (cache.exists(ipBlockKey(ip))) {
                throw new BizException(AuthErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("login protection check failed (fail-open), username={}", username, e);
        }
    }

    /**
     * Records a failed login. {@code userExists} distinguishes a real account
     * (counted against the per-account lockout) from a non-existent one (only the
     * IP counter is incremented, so attackers cannot manufacture locks for
     * arbitrary usernames).
     */
    public void onFailure(String username, String ip, boolean userExists) {
        if (!props.isEnabled()) {
            return;
        }
        try {
            if (userExists) {
                long attempts = cache.increment(failKey(username), props.getLockWindow());
                if (attempts >= props.getMaxAttempts()) {
                    long level = cache.increment(levelKey(username), props.getMaxLockDuration());
                    cache.put(lockKey(username), Long.toString(level), lockDuration(level));
                    cache.evict(failKey(username));
                    log.info("account locked username={} level={}", username, level);
                }
            }
            long ipAttempts = cache.increment(ipFailKey(ip), props.getIpWindow());
            if (ipAttempts >= props.getIpMaxAttempts()) {
                cache.put(ipBlockKey(ip), "1", props.getIpWindow());
            }
        } catch (Exception e) {
            log.warn("login protection onFailure failed (fail-open), username={}", username, e);
        }
    }

    /** Clears the account's lock state after a successful login. */
    public void onSuccess(String username, String ip) {
        if (!props.isEnabled()) {
            return;
        }
        try {
            cache.evict(failKey(username));
            cache.evict(lockKey(username));
            cache.evict(levelKey(username));
            cache.evict(ipFailKey(ip));
        } catch (Exception e) {
            log.warn("login protection onSuccess failed (fail-open), username={}", username, e);
        }
    }

    private Duration lockDuration(long level) {
        long minutes = props.getBaseLockDuration().toMinutes() * (1L << (level - 1));
        long cap = props.getMaxLockDuration().toMinutes();
        return Duration.ofMinutes(Math.min(minutes, cap));
    }

    private static String failKey(String username) {
        return "auth:fail:user:" + username;
    }

    private static String lockKey(String username) {
        return "auth:lockout:user:" + username;
    }

    private static String levelKey(String username) {
        return "auth:locklevel:user:" + username;
    }

    private static String ipFailKey(String ip) {
        return "auth:fail:ip:" + ip;
    }

    private static String ipBlockKey(String ip) {
        return "auth:block:ip:" + ip;
    }
}
