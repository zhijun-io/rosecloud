package io.rosecloud.system.service;

import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.service.dto.SettingValueRequest;

import java.util.List;

public interface SystemSettingService {

    List<SystemSetting> list();

    SystemSetting get(String key);

    void save(String key, SettingValueRequest request);

    void delete(String key);
}
