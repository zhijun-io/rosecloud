package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

/**
 * Repository port for roles. Implemented in the infrastructure layer; the
 * service depends only on this interface so persistence stays swappable.
 */
public interface RoleRepository {

    boolean existsByCode(String code);

    Long insert(Role role);

    PageResult<Role> page(long current, long size, String keyword);
}
