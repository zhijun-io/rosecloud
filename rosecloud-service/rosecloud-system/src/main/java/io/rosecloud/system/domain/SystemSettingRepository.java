package io.rosecloud.system.domain;

import java.util.List;
import java.util.Optional;

public interface SystemSettingRepository {

    Optional<SystemSetting> findByKey(String key);

    List<SystemSetting> findAll();

    void save(SystemSetting setting);

    void deleteByKey(String key);
}
