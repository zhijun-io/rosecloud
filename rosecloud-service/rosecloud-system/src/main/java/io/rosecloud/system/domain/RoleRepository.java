package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for roles. Implemented in the infrastructure layer; the
 * service depends only on this interface so persistence stays swappable.
 */
public interface RoleRepository {

    boolean existsByCode(String code);

    Long insert(Role role);

    PageResult<Role> page(long current, long size, String keyword);

    boolean existsById(Long id);

    Optional<Role> findById(Long id);

    Optional<Role> findByCode(String code);

    List<Long> findMenuIdsByRoleId(Long roleId);

    void assignMenus(Long roleId, Collection<Long> menuIds);
}
