package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.Optional;

/**
 * Repository port for tenants. Implemented in the infrastructure layer; the
 * service depends only on this interface so persistence stays swappable.
 */
public interface TenantRepository {

    Optional<Tenant> findById(String id);

    PageResult<Tenant> page(long current, long size, String keyword);

    String insert(Tenant tenant, String adminUsername, String adminPasswordHash);

    Optional<TenantAdminCredentials> findAdminCredentials(String id);

    void clearAdminPassword(String id);

    void updateStatus(String id, TenantStatus status);
}
