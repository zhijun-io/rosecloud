package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SettingKeyMapper;
import io.rosecloud.system.persistence.UserSettingEntity;
import io.rosecloud.system.persistence.UserSettingMapper;
import io.rosecloud.system.service.UserSettingService;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserSettingServiceImpl implements UserSettingService {

    private final UserSettingMapper userSettingMapper;
    private final SettingKeyMapper settingKeyMapper;

    public UserSettingServiceImpl(UserSettingMapper userSettingMapper,
                                 SettingKeyMapper settingKeyMapper) {
        this.userSettingMapper = userSettingMapper;
        this.settingKeyMapper = settingKeyMapper;
    }

    @Override
    public List<UserSetting> listMine() {
        return userSettingMapper.selectList(new LambdaQueryWrapper<UserSettingEntity>()
                        .eq(UserSettingEntity::getUserId, currentUserId())
                        .orderByAsc(UserSettingEntity::getSettingKey)).stream().map(UserSettingEntity::toData).toList();
    }

    @Override
    public UserSetting getMine(String key) {
        return findByUserIdAndKey(currentUserId(), key)
                .orElseThrow(() -> new BizException(SystemErrorCode.USER_SETTING_NOT_FOUND));
    }

    @AuditLog(action = "user-setting-save", description = "保存用户配置")
    @Override
    public void saveMine(String key, SettingValueRequest request) {
        Long userId = currentUserId();
        ensureSettingKeyExists(key);
        UserSettingEntity existing = userSettingMapper.selectOne(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getUserId, userId)
                .eq(UserSettingEntity::getSettingKey, key));
        UserSettingEntity po = new UserSettingEntity().toEntity(new UserSetting(userId, key, request.value(), now(), userId));
        if (existing == null) {
            userSettingMapper.insert(po);
            return;
        }
        po.setId(existing.getId());
        userSettingMapper.updateById(po);
    }

    @AuditLog(action = "user-setting-delete", description = "删除用户配置")
    @Override
    public void deleteMine(String key) {
        Long userId = currentUserId();
        if (findByUserIdAndKey(userId, key).isEmpty()) {
            throw new BizException(SystemErrorCode.USER_SETTING_NOT_FOUND);
        }
        userSettingMapper.delete(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getUserId, userId)
                .eq(UserSettingEntity::getSettingKey, key));
    }

    private Optional<UserSetting> findByUserIdAndKey(Long userId, String key) {
        return Optional.ofNullable(userSettingMapper.selectOne(new LambdaQueryWrapper<UserSettingEntity>()
                        .eq(UserSettingEntity::getUserId, userId)
                        .eq(UserSettingEntity::getSettingKey, key)))
                .map(UserSettingEntity::toData);
    }

    private void ensureSettingKeyExists(String key) {
        if (settingKeyMapper.selectOne(new LambdaQueryWrapper<SettingKeyEntity>()
                .eq(SettingKeyEntity::getKey, key)) == null) {
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
