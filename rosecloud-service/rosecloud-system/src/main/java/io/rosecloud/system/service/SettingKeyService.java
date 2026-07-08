package io.rosecloud.system.service;

import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.service.dto.SettingKeyCreateRequest;
import io.rosecloud.system.service.dto.SettingKeyUpdateRequest;

import java.util.List;

public interface SettingKeyService {

    String create(SettingKeyCreateRequest request);

    void update(String key, SettingKeyUpdateRequest request);

    void delete(String key);

    SettingKey get(String key);

    List<SettingKey> list();

    PageResult<SettingKey> page(long current, long size, String keyword);
}
