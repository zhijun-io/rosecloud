package io.rosecloud.system.domain;

import java.util.List;
import java.util.Optional;

public interface UserSettingRepository {

    Optional<UserSetting> findByUserIdAndKey(Long userId, String key);

    List<UserSetting> findByUserId(Long userId);

    void save(UserSetting setting);

    void deleteByUserIdAndKey(Long userId, String key);

    void deleteByKey(String key);
}
