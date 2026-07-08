package io.rosecloud.system.service;

import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.service.dto.SettingValueRequest;

import java.util.List;

public interface UserSettingService {

    List<UserSetting> listMine();

    UserSetting getMine(String key);

    void saveMine(String key, SettingValueRequest request);

    void deleteMine(String key);
}
