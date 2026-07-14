package io.rosecloud.system.service.impl;

import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyDao;
import io.rosecloud.system.persistence.UserSettingDao;
import io.rosecloud.system.persistence.UserSettingEntity;
import io.rosecloud.system.service.UserSettingService;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UserSettingServiceImpl implements UserSettingService {

    private final UserSettingDao userSettingDao;
    private final SettingKeyDao settingKeyDao;

    @Override
    public List<UserSetting> listMine() {
        return userSettingDao.listByUserId(currentUserId());
    }

    @Override
    public UserSetting getMine(String key) {
        return userSettingDao.findByUserIdAndKey(currentUserId(), key)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_SETTING_NOT_FOUND));
    }

    @AuditLog(action = "user-setting-save", description = "保存用户配置")
    @Override
    public void saveMine(String key, SettingValueRequest request) {
        Long userId = currentUserId();
        ensureSettingKeyExists(key);
        UserSettingEntity existing = userSettingDao.findEntityByUserIdAndKey(userId, key);
        UserSettingEntity po = new UserSettingEntity().toEntity(new UserSetting(userId, key, request.value(), now(), userId));
        if (existing == null) {
            userSettingDao.insert(po);
            return;
        }
        po.setId(existing.getId());
        userSettingDao.updateById(po);
    }

    @AuditLog(action = "user-setting-delete", description = "删除用户配置")
    @Override
    public void deleteMine(String key) {
        Long userId = currentUserId();
        if (userSettingDao.findByUserIdAndKey(userId, key).isEmpty()) {
            throw new BizException(SystemErrorCode.USER_SETTING_NOT_FOUND);
        }
        userSettingDao.deleteByUserIdAndKey(userId, key);
    }

    private void ensureSettingKeyExists(String key) {
        if (settingKeyDao.findByKey(key).isEmpty()) {
            throw new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND);
        }
    }

    private static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof io.rosecloud.common.security.model.SecurityUser su) || su.getUserId() == null) {
            throw new BizException(SecurityErrorCode.UNAUTHORIZED);
        }
        return su.getUserId();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
