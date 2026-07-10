package io.rosecloud.api.user;

import io.rosecloud.common.core.model.ApiResponse;

import java.util.List;

/**
 * Tenant-membership contract owned by the system service, consumed by the auth
 * service to support controlled tenant switching.
 */
public interface UserTenantApi {

    /** Tenant ids the user is a member of. Platform admins receive all tenant ids. */
    ApiResponse<List<String>> listTenantIds(Long userId);

    /** Tenant summaries the user can see. Auth filters these into switchable tenants. */
    ApiResponse<List<TenantAccessCandidate>> listTenantCandidates(Long userId);

    /** Whether the user is a platform admin (belongs to the system tenant). */
    ApiResponse<Boolean> isPlatformAdmin(Long userId);
}
