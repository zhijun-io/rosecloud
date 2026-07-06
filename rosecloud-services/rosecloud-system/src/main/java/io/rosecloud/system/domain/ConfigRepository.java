package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.Optional;

/** Repository port for business configuration entries. */
public interface ConfigRepository {

    boolean existsByKey(String key);

    Long insert(Config config);

    void update(Config config);

    Optional<Config> findById(Long id);

    Optional<Config> findByKey(String key);

    void deleteById(Long id);

    PageResult<Config> page(long current, long size, String keyword);
}
