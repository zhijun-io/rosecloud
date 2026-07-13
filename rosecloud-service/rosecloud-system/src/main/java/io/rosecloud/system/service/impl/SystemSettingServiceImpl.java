package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SettingKeyMapper;
import io.rosecloud.system.persistence.SystemSettingEntity;
import io.rosecloud.system.persistence.SystemSettingMapper;
import io.rosecloud.system.service.SystemSettingService;
import io.rosecloud.system.service.dto.SettingValueRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingMapper systemSettingMapper;
    private final SettingKeyMapper settingKeyMapper;

    public SystemSettingServiceImpl(SystemSettingMapper systemSettingMapper,
                                   SettingKeyMapper settingKeyMapper) {
        this.systemSettingMapper = systemSettingMapper;
        this.settingKeyMapper = settingKeyMapper;
    }

    @Override
    public List<SystemSetting> list() {
        return systemSettingMapper.selectList(new LambdaQueryWrapper<SystemSettingEntity>()
                        .orderByAsc(SystemSettingEntity::getSettingKey)).stream().map(SystemSettingEntity::toData).toList();
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
        SystemSettingEntity existing = systemSettingMapper.selectById(key);
        SystemSettingEntity po = new SystemSettingEntity().toEntity(new SystemSetting(key, request.value(), now(), currentUserId()));
        if (existing == null) {
            systemSettingMapper.insert(po);
            return;
        }
        systemSettingMapper.updateById(po);
    }

    @AuditLog(action = "system-setting-delete", description = "删除系统配置")
    @Override
    public void delete(String key) {
        ensureSettingKeyExists(key);
        if (findByKey(key).isEmpty()) {
            throw new BizException(SystemErrorCode.SYSTEM_SETTING_NOT_FOUND);
        }
        systemSettingMapper.deleteById(key);
    }

    private Optional<SystemSetting> findByKey(String key) {
        return Optional.ofNullable(systemSettingMapper.selectById(key)).map(SystemSettingEntity::toData);
    }

    private void ensureSettingKeyExists(String key) {
        if (settingKeyMapper.selectOne(new LambdaQueryWrapper<SettingKeyEntity>()
                .eq(SettingKeyEntity::getKey, key)) == null) {
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
