package io.rosecloud.system.domain;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.error.SystemErrorCode;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Tenant lifecycle status (stored as a tinyint code). */
public enum TenantStatus {

    PENDING(0),
    ENABLED(1),
    DISABLED(2),
    EXPIRED(3);

    private final int code;

    TenantStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static TenantStatus of(int code) {
        for (TenantStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown tenant status: " + code);
    }

    /**
     * Resolves the effective status for a stored code plus expiry date. A past
     * {@code expireTime} overrides the stored status with {@link #EXPIRED}; a
     * {@code null} code yields {@code null}.
     */
    public static TenantStatus resolve(Integer code, LocalDate expireTime) {
        if (code == null) {
            return null;
        }
        TenantStatus status = of(code);
        if (expireTime != null && expireTime.isBefore(LocalDate.now())) {
            return EXPIRED;
        }
        return status;
    }

    /**
     * Legal transitions. {@code PENDING -> ENABLED/DISABLED}; {@code ENABLED <-> DISABLED}
     * on disable/enable; {@code * -> EXPIRED} when the tenant expires. Centralizing
     * the rules here keeps every caller's state change in one auditable place.
     */
    private static final Map<TenantStatus, Set<TenantStatus>> TRANSITIONS = new EnumMap<>(TenantStatus.class);

    static {
        TRANSITIONS.put(PENDING, EnumSet.of(ENABLED, DISABLED, EXPIRED));
        TRANSITIONS.put(ENABLED, EnumSet.of(DISABLED, EXPIRED));
        TRANSITIONS.put(DISABLED, EnumSet.of(ENABLED, EXPIRED));
        TRANSITIONS.put(EXPIRED, EnumSet.noneOf(TenantStatus.class));
    }

    /** Returns the next status for an open (provision) action; throws if not allowed. */
    public TenantStatus open() {
        return transitionTo(ENABLED);
    }

    /** Returns the next status for a disable action; throws if not allowed. */
    public TenantStatus disable() {
        return transitionTo(DISABLED);
    }

    /** Returns the next status for an enable action; throws if not allowed. */
    public TenantStatus enable() {
        return transitionTo(ENABLED);
    }

    /** Returns the terminal expired status; throws if the transition is not allowed. */
    public TenantStatus expire() {
        return transitionTo(EXPIRED);
    }

    /**
     * Validates that {@code this -> target} is allowed and returns {@code target};
     * otherwise throws {@link BizException} with {@code system.tenant_status_invalid}.
     */
    public TenantStatus transitionTo(TenantStatus target) {
        if (target == EXPIRED) {
            return target;
        }
        Set<TenantStatus> allowed = TRANSITIONS.get(this);
        if (allowed == null || !allowed.contains(target)) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        return target;
    }
}
