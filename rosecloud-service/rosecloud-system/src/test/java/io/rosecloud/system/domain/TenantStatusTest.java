package io.rosecloud.system.domain;

import io.rosecloud.common.core.error.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantStatusTest {

    @Test
    void openTransitionsPendingToEnabled() {
        assertEquals(TenantStatus.ENABLED, TenantStatus.PENDING.open());
    }

    @Test
    void enableTransitionsDisabledToEnabled() {
        assertEquals(TenantStatus.ENABLED, TenantStatus.DISABLED.enable());
    }

    @Test
    void disableTransitionsEnabledToDisabled() {
        assertEquals(TenantStatus.DISABLED, TenantStatus.ENABLED.disable());
    }

    @Test
    void pendingCanBeDisabledOrEnabledDirectly() {
        assertEquals(TenantStatus.DISABLED, TenantStatus.PENDING.disable());
        assertEquals(TenantStatus.ENABLED, TenantStatus.PENDING.enable());
    }

    @Test
    void transitionToRejectsIllegalTargets() {
        assertThrows(BizException.class, () -> TenantStatus.PENDING.transitionTo(TenantStatus.PENDING));
        assertThrows(BizException.class, () -> TenantStatus.ENABLED.transitionTo(TenantStatus.PENDING));
        assertThrows(BizException.class, () -> TenantStatus.DISABLED.transitionTo(TenantStatus.PENDING));
    }

    @Test
    void enabledCannotBeEnabledAgain() {
        assertThrows(BizException.class, () -> TenantStatus.ENABLED.enable());
    }

    @Test
    void disabledCannotBeDisabledAgain() {
        assertThrows(BizException.class, () -> TenantStatus.DISABLED.disable());
    }

    @Test
    void expiredIsTerminal() {
        assertThrows(BizException.class, () -> TenantStatus.EXPIRED.open());
        assertThrows(BizException.class, () -> TenantStatus.EXPIRED.enable());
        assertThrows(BizException.class, () -> TenantStatus.EXPIRED.disable());
        assertEquals(TenantStatus.EXPIRED, TenantStatus.EXPIRED.expire());
    }

    @Test
    void anyStatusCanExpire() {
        assertEquals(TenantStatus.EXPIRED, TenantStatus.PENDING.expire());
        assertEquals(TenantStatus.EXPIRED, TenantStatus.ENABLED.expire());
        assertEquals(TenantStatus.EXPIRED, TenantStatus.DISABLED.expire());
    }

    @Test
    void transitionToAlwaysAllowsExpire() {
        assertEquals(TenantStatus.EXPIRED, TenantStatus.PENDING.transitionTo(TenantStatus.EXPIRED));
        assertEquals(TenantStatus.EXPIRED, TenantStatus.ENABLED.transitionTo(TenantStatus.EXPIRED));
        assertEquals(TenantStatus.EXPIRED, TenantStatus.DISABLED.transitionTo(TenantStatus.EXPIRED));
        assertEquals(TenantStatus.EXPIRED, TenantStatus.EXPIRED.transitionTo(TenantStatus.EXPIRED));
    }
}
