package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.system.domain.DictData;
import io.rosecloud.system.service.dto.DictDataRequest;

import java.util.List;

public interface DictDataService {

    Long create(DictDataRequest request);

    void update(Long id, DictDataRequest request);

    void delete(Long id);

    DictData get(Long id);

    List<DictData> listByCode(String dictCode);

    PagedData<DictData> page(PageQuery pageQuery, String dictCode);
}
