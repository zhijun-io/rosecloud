package io.rosecloud.system.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for menus. Implemented in the infrastructure layer; the
 * service depends only on this interface so persistence stays swappable.
 */
public interface MenuRepository {

    Optional<Menu> findById(Long id);

    boolean existsByParentId(Long parentId);

    Long insert(Menu menu);

    void update(Menu menu);

    void deleteById(Long id);

    List<Menu> findAll();

    List<Menu> findByRoleIds(Collection<Long> roleIds);
}
