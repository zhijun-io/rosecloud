package io.rosecloud.auth.service.security;

import io.rosecloud.auth.config.LoginProtectionProperties;
import io.rosecloud.auth.error.AuthErrorCode;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.cache.LocalRoseCloudCache;
import io.rosecloud.starter.cache.RoseCloudCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginProtectionServiceTest {

    private final RoseCloudCache cache = new LocalRoseCloudCache();
    private final LoginProtectionProperties props = new LoginProtectionProperties();

    private LoginProtectionService service() {
        return new LoginProtectionService(cache, props);
    }

    @Test
    void disabledDoesNothing() {
        props.setEnabled(false);
        LoginProtectionService s = service();
        s.onFailure("u", "1.2.3.4", true);
        s.checkAllowed("u", "1.2.3.4");
    }

    @Test
    void locksAccountAfterMaxAttempts() {
        LoginProtectionService s = service();
        for (int i = 0; i < props.getMaxAttempts(); i++) {
            s.onFailure("alice", "1.1.1.1", true);
        }
        BizException ex = assertThrows(BizException.class, () -> s.checkAllowed("alice", "1.1.1.1"));
        assertEquals(AuthErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
    }

    @Test
    void onSuccessClearsLock() {
        LoginProtectionService s = service();
        for (int i = 0; i < props.getMaxAttempts(); i++) {
            s.onFailure("alice", "1.1.1.1", true);
        }
        assertThrows(BizException.class, () -> s.checkAllowed("alice", "1.1.1.1"));
        s.onSuccess("alice", "1.1.1.1");
        s.checkAllowed("alice", "1.1.1.1");
    }

    @Test
    void blocksIpAfterMaxAttempts() {
        LoginProtectionService s = service();
        for (int i = 0; i < props.getIpMaxAttempts(); i++) {
            s.onFailure("ghost" + i, "9.9.9.9", false);
        }
        BizException ex = assertThrows(BizException.class, () -> s.checkAllowed("anyone", "9.9.9.9"));
        assertEquals(AuthErrorCode.TOO_MANY_REQUESTS, ex.getErrorCode());
    }

    @Test
    void escalatesLockLevel() {
        LoginProtectionService s = service();
        for (int i = 0; i < props.getMaxAttempts(); i++) {
            s.onFailure("bob", "2.2.2.2", true);
        }
        assertEquals("1", cache.get("auth:locklevel:user:bob"));
        cache.evict("auth:lockout:user:bob");
        for (int i = 0; i < props.getMaxAttempts(); i++) {
            s.onFailure("bob", "2.2.2.2", true);
        }
        assertEquals("2", cache.get("auth:locklevel:user:bob"));
    }

    @Test
    void nonExistentUserNeverLocksAccount() {
        LoginProtectionService s = service();
        for (int i = 0; i < props.getMaxAttempts() + 5; i++) {
            s.onFailure("nope", "3.3.3.3", false);
        }
        assertNull(cache.get("auth:lockout:user:nope"));
        s.checkAllowed("nope", "3.3.3.3");
    }
}
