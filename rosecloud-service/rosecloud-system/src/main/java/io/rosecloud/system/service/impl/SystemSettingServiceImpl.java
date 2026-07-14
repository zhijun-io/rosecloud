package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyDao;
import io.rosecloud.system.persistence.SystemSettingDao;
import io.rosecloud.system.service.SystemSettingService;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingDao systemSettingDao;
    private final SettingKeyDao settingKeyDao;
    @Override
    public List<SystemSetting> list() {
        return systemSettingDao.findAllOrderByKey();
    }

    @Override
    public SystemSetting get(String key) {
        return findByKey(key)
                .orElseThrow(() -> new BizException(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND));
    }

    @AuditLog(action = "system-setting-save", description = "保存系统配置")
    @Override
    public void save(String key, SettingValueRequest request) {
        ensureSettingKeyExists(key);
        systemSettingDao.save(new SystemSetting(key, request.value(), now(), currentUserId()));
    }

    @AuditLog(action = "system-setting-delete", description = "删除系统配置")
    @Override
    public void delete(String key) {
        ensureSettingKeyExists(key);
        if (findByKey(key).isEmpty()) {
            throw new BizException(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND);
        }
        systemSettingDao.removeById(key);
    }

    private Optional<SystemSetting> findByKey(String key) {
        return systemSettingDao.findById(key);
    }

    private void ensureSettingKeyExists(String key) {
        if (!settingKeyDao.existsByKey(key)) {
            throw new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND);
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now();
    }

    private static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof io.rosecloud.common.security.model.SecurityUser su)) {
            return null;
        }
        return su.getUserId();
    }

}
