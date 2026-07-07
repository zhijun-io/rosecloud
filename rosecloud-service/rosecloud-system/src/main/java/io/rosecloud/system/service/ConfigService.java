package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Config;
import io.rosecloud.system.service.dto.ConfigRequest;

public interface ConfigService {

    Long create(ConfigRequest request);

    void update(Long id, ConfigRequest request);

    void delete(Long id);

    Config get(Long id);

    Config getByKey(String key);

    PageResult<Config> page(long current, long size, String keyword);
}
