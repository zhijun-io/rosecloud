package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.SettingKeyRepository;
import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.domain.SystemSettingRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.SystemSettingService;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SettingKeyRepository settingKeyRepository;
    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingServiceImpl(SettingKeyRepository settingKeyRepository,
                                   SystemSettingRepository systemSettingRepository) {
        this.settingKeyRepository = settingKeyRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Override
    public List<SystemSetting> list() {
        return systemSettingRepository.findAll();
    }

    @Override
    public SystemSetting get(String key) {
        return systemSettingRepository.findByKey(key)
                .orElseThrow(() -> new BizException(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND));
    }

    @AuditLog(action = "system-setting-save", description = "保存系统配置")
    @Override
    public void save(String key, SettingValueRequest request) {
        ensureSettingKeyExists(key);
        systemSettingRepository.save(new SystemSetting(key, request.value(), now(), currentUserId()));
    }

    @AuditLog(action = "system-setting-delete", description = "删除系统配置")
    @Override
    public void delete(String key) {
        ensureSettingKeyExists(key);
        if (systemSettingRepository.findByKey(key).isEmpty()) {
            throw new BizException(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND);
        }
        systemSettingRepository.deleteByKey(key);
    }

    private void ensureSettingKeyExists(String key) {
        if (settingKeyRepository.findByKey(key).isEmpty()) {
            throw new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND);
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now();
    }

    private static Long currentUserId() {
        CurrentUser current = UserContext.get();
        return current == null ? null : current.userId();
    }
}
