package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.security.SecurityErrorCode;
import io.rosecloud.system.domain.SettingKeyRepository;
import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.domain.UserSettingRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.UserSettingService;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserSettingServiceImpl implements UserSettingService {

    private final SettingKeyRepository settingKeyRepository;
    private final UserSettingRepository userSettingRepository;

    public UserSettingServiceImpl(SettingKeyRepository settingKeyRepository,
                                 UserSettingRepository userSettingRepository) {
        this.settingKeyRepository = settingKeyRepository;
        this.userSettingRepository = userSettingRepository;
    }

    @Override
    public List<UserSetting> listMine() {
        return userSettingRepository.findByUserId(currentUserId());
    }

    @Override
    public UserSetting getMine(String key) {
        return userSettingRepository.findByUserIdAndKey(currentUserId(), key)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_SETTING_NOT_FOUND));
    }

    @AuditLog(action = "user-setting-save", description = "保存用户配置")
    @Override
    public void saveMine(String key, SettingValueRequest request) {
        Long userId = currentUserId();
        ensureSettingKeyExists(key);
        userSettingRepository.save(new UserSetting(userId, key, request.value(), now(), userId));
    }

    @AuditLog(action = "user-setting-delete", description = "删除用户配置")
    @Override
    public void deleteMine(String key) {
        Long userId = currentUserId();
        if (userSettingRepository.findByUserIdAndKey(userId, key).isEmpty()) {
            throw new BizException(SystemErrorCode.USER_SETTING_NOT_FOUND);
        }
        userSettingRepository.deleteByUserIdAndKey(userId, key);
    }

    private void ensureSettingKeyExists(String key) {
        if (settingKeyRepository.findByKey(key).isEmpty()) {
            throw new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND);
        }
    }

    private static Long currentUserId() {
        CurrentUser current = UserContext.get();
        if (current == null || current.userId() == null) {
            throw new BizException(SecurityErrorCode.UNAUTHORIZED);
        }
        return current.userId();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
