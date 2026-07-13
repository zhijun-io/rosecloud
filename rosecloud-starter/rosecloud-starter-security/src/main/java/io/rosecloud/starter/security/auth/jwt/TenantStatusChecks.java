package io.rosecloud.starter.security.auth.jwt;

 import io.rosecloud.common.security.user.TenantLookupApi;
 import io.rosecloud.common.security.user.TenantStatusView;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.starter.tenant.core.TenantContextHolder;

/**
 * Shared guard that rejects authentication when the active tenant is
 * {@code DISABLED} or {@code PENDING} — both of which must never establish
 * a session.  {@code STOPPED}/{@code EXPIRED} are allowed through so the
 * user retains read-only access to their existing session; write operations
 * are blocked separately at the service/permission layer.
 *
 * <p>The lookup is skipped for the system tenant and when no
 * {@link TenantLookupApi} bean is available (non-auth services that do not
 * enable Feign scanning for the API package).
 */
final class TenantStatusChecks {

    private TenantStatusChecks() {
    }

    static void requireEnabled(String tenantId, TenantLookupApi tenantLookupApi) {
        if (tenantLookupApi == null || TenantContextHolder.SYSTEM_TENANT_ID.equals(tenantId)) {
            return;
        }
        ApiResponse<TenantStatusView> response = tenantLookupApi.findTenantStatus(tenantId);
        if (response == null || !response.success() || response.data() == null) {
            throw new BizException(SecurityErrorCode.TENANT_UNAVAILABLE);
        }
        String status = response.data().tenantStatus();
        if ("DISABLED".equalsIgnoreCase(status)) {
            throw new BizException(SecurityErrorCode.TENANT_DISABLED);
        }
        if ("PENDING".equalsIgnoreCase(status) || "PENDING_ACTIVATION".equalsIgnoreCase(status)) {
            throw new BizException(SecurityErrorCode.TENANT_PENDING);
        }
        // STOPPED / EXPIRED are allowed (read-only session); writes blocked separately.
    }
}
