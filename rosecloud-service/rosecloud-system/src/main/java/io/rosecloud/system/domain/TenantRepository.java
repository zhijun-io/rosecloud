package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.Optional;

/**
 * Repository port for tenants. Implemented in the infrastructure layer; the
 * service depends only on this interface so persistence stays swappable.
 */
public interface TenantRepository {

    Optional<Tenant> findById(Long id);

    boolean existsByCode(String code);

    Long insert(Tenant tenant, String adminUsername, String adminPasswordHash);

    Optional<TenantAdminCredentials> findAdminCredentials(Long id);

    void clearAdminPassword(Long id);

    void updateStatus(Long id, TenantStatus status);

    PageResult<Tenant> page(long current, long size, String keyword);
}
