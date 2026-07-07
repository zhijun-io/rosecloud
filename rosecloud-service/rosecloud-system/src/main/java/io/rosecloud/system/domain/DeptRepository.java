package io.rosecloud.system.domain;

import java.util.List;
import java.util.Optional;

/** Repository port for departments. */
public interface DeptRepository {

    Optional<Dept> findById(Long id);

    boolean existsByParentId(Long parentId);

    Long insert(Dept dept);

    void update(Dept dept);

    void deleteById(Long id);

    List<Dept> findAll();
}
