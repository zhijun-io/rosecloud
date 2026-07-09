package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.SettingKeyRepository;
import io.rosecloud.system.domain.SystemSettingRepository;
import io.rosecloud.system.domain.UserSettingRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.SettingKeyService;
import io.rosecloud.system.service.dto.SettingKeyCreateRequest;
import io.rosecloud.system.service.dto.SettingKeyUpdateRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettingKeyServiceImpl implements SettingKeyService {

    private final SettingKeyRepository settingKeyRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final UserSettingRepository userSettingRepository;

    public SettingKeyServiceImpl(SettingKeyRepository settingKeyRepository,
                                 SystemSettingRepository systemSettingRepository,
                                 UserSettingRepository userSettingRepository) {
        this.settingKeyRepository = settingKeyRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.userSettingRepository = userSettingRepository;
    }

    @AuditLog(action = "setting-key-create", description = "创建配置键")
    @Override
    public String create(SettingKeyCreateRequest request) {
        if (settingKeyRepository.existsByKey(request.key())) {
            throw new BizException(SystemErrorCode.SETTING_KEY_EXISTS);
        }
        Long userId = currentUserId();
        LocalDateTime now = now();
        settingKeyRepository.insert(new SettingKey(null, request.key(), request.name(), request.remark(),
                now, userId, now, userId));
        return request.key();
    }

    @AuditLog(action = "setting-key-update", description = "修改配置键")
    @Override
    public void update(String key, SettingKeyUpdateRequest request) {
        SettingKey current = load(key);
        Long userId = currentUserId();
        settingKeyRepository.update(new SettingKey(current.getId(), key, request.name(), request.remark(),
                current.getCreateTime(), current.getCreateBy(), now(), userId));
    }

    @AuditLog(action = "setting-key-delete", description = "删除配置键")
    @Transactional
    @Override
    public void delete(String key) {
        load(key);
        systemSettingRepository.deleteByKey(key);
        userSettingRepository.deleteByKey(key);
        settingKeyRepository.deleteByKey(key);
    }

    @Override
    public SettingKey get(String key) {
        return load(key);
    }

    @Override
    public List<SettingKey> list() {
        return settingKeyRepository.findAll();
    }

    @Override
    public PageResult<SettingKey> page(long current, long size, String keyword) {
        return settingKeyRepository.page(current, size, keyword);
    }

    private SettingKey load(String key) {
        return settingKeyRepository.findByKey(key)
                .orElseThrow(() -> new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND));
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
