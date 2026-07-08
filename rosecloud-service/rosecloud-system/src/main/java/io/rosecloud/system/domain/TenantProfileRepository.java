package io.rosecloud.system.domain;

import java.util.List;
import java.util.Optional;

/** Repository port for tenant profiles. */
public interface TenantProfileRepository {

    Optional<TenantProfile> findById(String id);

    Optional<TenantProfile> findDefault();

    List<TenantProfile> findAll();

    boolean existsById(String id);

    void insert(TenantProfile profile);

    void update(TenantProfile profile);

    void deleteById(String id);

    void makeDefault(String id);

    default String defaultProfileId() {
        return findDefault().map(TenantProfile::getId)
                .orElseThrow(() -> new IllegalStateException("default tenant profile not found"));
    }
}
