package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface SettingKeyRepository {

    boolean existsByKey(String key);

    void insert(SettingKey settingKey);

    void update(SettingKey settingKey);

    Optional<SettingKey> findByKey(String key);

    void deleteByKey(String key);

    List<SettingKey> findAll();

    PageResult<SettingKey> page(long current, long size, String keyword);
}
