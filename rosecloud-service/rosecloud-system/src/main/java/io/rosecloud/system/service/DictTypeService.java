package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.DictType;
import io.rosecloud.system.service.dto.DictTypeRequest;

public interface DictTypeService {

    Long create(DictTypeRequest request);

    void update(Long id, DictTypeRequest request);

    void delete(Long id);

    DictType get(Long id);

    PageResult<DictType> page(long current, long size, String keyword);
}
