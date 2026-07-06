package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Config;
import io.rosecloud.system.domain.ConfigRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.ConfigService;
import io.rosecloud.system.service.dto.ConfigRequest;
import org.springframework.stereotype.Service;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final ConfigRepository configRepository;

    public ConfigServiceImpl(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @AuditLog(action = "config-create", description = "创建参数配置")
    @Override
    public Long create(ConfigRequest request) {
        if (configRepository.existsByKey(request.configKey())) {
            throw new BizException(SystemErrorCode.CONFIG_KEY_EXISTS);
        }
        return configRepository.insert(new Config(null, request.configKey(), request.configValue(), request.description()));
    }

    @AuditLog(action = "config-update", description = "修改参数配置")
    @Override
    public void update(Long id, ConfigRequest request) {
        configRepository.findById(id).orElseThrow(() -> new BizException(SystemErrorCode.CONFIG_NOT_FOUND));
        configRepository.update(new Config(id, request.configKey(), request.configValue(), request.description()));
    }

    @AuditLog(action = "config-delete", description = "删除参数配置")
    @Override
    public void delete(Long id) {
        configRepository.deleteById(id);
    }

    @Override
    public Config get(Long id) {
        return configRepository.findById(id).orElseThrow(() -> new BizException(SystemErrorCode.CONFIG_NOT_FOUND));
    }

    @Override
    public Config getByKey(String key) {
        return configRepository.findByKey(key).orElseThrow(() -> new BizException(SystemErrorCode.CONFIG_NOT_FOUND));
    }

    @Override
    public PageResult<Config> page(long current, long size, String keyword) {
        return configRepository.page(current, size, keyword);
    }
}
