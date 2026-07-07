package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.List;
import java.util.Optional;

/** Repository port for dictionary items. */
public interface DictDataRepository {

    Long insert(DictData dictData);

    void update(DictData dictData);

    Optional<DictData> findById(Long id);

    List<DictData> findByDictCode(String dictCode);

    List<DictData> findEnabledByDictCode(String dictCode);

    void deleteById(Long id);

    void deleteByDictCode(String dictCode);

    PageResult<DictData> page(long current, long size, String dictCode);
}
