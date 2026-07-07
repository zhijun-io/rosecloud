package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.Optional;

/** Repository port for dictionary types. */
public interface DictTypeRepository {

    boolean existsByCode(String code);

    Long insert(DictType dictType);

    void update(DictType dictType);

    Optional<DictType> findById(Long id);

    Optional<DictType> findByCode(String code);

    void deleteById(Long id);

    PageResult<DictType> page(long current, long size, String keyword);
}
