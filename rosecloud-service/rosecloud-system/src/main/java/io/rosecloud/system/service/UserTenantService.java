package io.rosecloud.system.service;

import io.rosecloud.api.user.TenantAccessCandidate;

import java.util.List;

/**
 * Service contract for tenant-membership queries used by the auth service.
 */
public interface UserTenantService {

    /** Tenant IDs the user is a member of. Platform admins receive all tenant IDs. */
    List<String> listTenantIds(Long userId);

    /** Tenant summaries the user can see. Auth filters these into switchable tenants. */
    List<TenantAccessCandidate> listTenantCandidates(Long userId);

    /** Whether the user is a platform admin (belongs to the system tenant). */
    boolean isPlatformAdmin(Long userId);
}
