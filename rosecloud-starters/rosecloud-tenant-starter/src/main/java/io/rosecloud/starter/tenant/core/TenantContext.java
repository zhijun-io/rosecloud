package io.rosecloud.starter.tenant.core;

/**
 * Holds the current tenant id for the duration of a request.
 *
 * <p>Populated by the tenant web/gateway filter, propagated across async
 * boundaries by {@code TenantContextTaskDecorator}. Callers must clear it
 * when the unit of work ends.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static boolean hasTenant() {
        return TENANT_ID.get() != null;
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
