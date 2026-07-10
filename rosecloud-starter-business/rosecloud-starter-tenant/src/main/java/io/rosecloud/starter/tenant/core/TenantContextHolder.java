package io.rosecloud.starter.tenant.core;

/**
 * Holds the current tenant id for the duration of a request.
 *
 * <p>Populated by the tenant web/gateway filter, propagated across async
 * boundaries by {@code TenantContextTaskDecorator}. Callers must clear it
 * when the unit of work ends.
 */
public final class TenantContextHolder {

    /**
     * 默认系统租户（平台管理员所属）。保留租户，不可经普通创建/删除流程变更。
     * 活动租户为该值时，视为「平台视角」，行级隔离豁免。
     */
    public static final String SYSTEM_TENANT_ID = "ROOT";

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static boolean hasTenant() {
        return TENANT_ID.get() != null;
    }

    public static boolean isSystemTenant() {
        return SYSTEM_TENANT_ID.equals(TENANT_ID.get());
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
