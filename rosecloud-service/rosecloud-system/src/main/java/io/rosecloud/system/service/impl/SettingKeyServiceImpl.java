package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyDao;
import io.rosecloud.system.persistence.UserSettingEntity;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.UserSettingMapper;
import io.rosecloud.system.persistence.SystemSettingDao;
import io.rosecloud.system.service.SettingKeyService;
import io.rosecloud.system.service.dto.SettingKeyCreateRequest;
import io.rosecloud.system.service.dto.SettingKeyUpdateRequest;
import io.rosecloud.system.service.validator.SettingKeyValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class SettingKeyServiceImpl implements SettingKeyService {

    private final SettingKeyDao settingKeyDao;
    private final SettingKeyValidator settingKeyValidator;
    private final UserSettingMapper userSettingMapper;
    private final SystemSettingDao systemSettingDao;

    @AuditLog(action = "setting-key-create", description = "创建配置键")
    @Override
    public String create(SettingKeyCreateRequest request) {
        Long userId = currentUserId();
        LocalDateTime now = now();
        SettingKey settingKey = new SettingKey(null, request.key(), request.name(), request.remark(),
                now, userId, now, userId);
        settingKeyValidator.validateCreate(settingKey);
        settingKeyDao.save(settingKey);
        return request.key();
    }

    @AuditLog(action = "setting-key-update", description = "修改配置键")
    @Override
    public void update(String key, SettingKeyUpdateRequest request) {
        SettingKey current = findByKey(key)
                .orElseThrow(() -> new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND));
        Long userId = currentUserId();
        SettingKey updated = new SettingKey(current.getId(), key, request.name(), request.remark(),
                current.getCreateTime(), current.getCreateBy(), now(), userId);
        settingKeyDao.save(updated);
    }

    @AuditLog(action = "setting-key-delete", description = "删除配置键")
    @Transactional
    @Override
    public void delete(String key) {
        SettingKey current = findByKey(key)
                .orElseThrow(() -> new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND));
        systemSettingDao.removeById(key);
        userSettingMapper.delete(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getSettingKey, key));
        settingKeyDao.removeById(current.getId());
    }

    @Override
    public SettingKey get(String key) {
        return findByKey(key)
                .orElseThrow(() -> new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND));
    }

    @Override
    public List<SettingKey> list() {
        return settingKeyDao.findAll().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .toList();
    }

    @Override
    public PagedData<SettingKey> page(PageQuery pageQuery) {
        return settingKeyDao.page(pageQuery,
                q -> {
                    LambdaQueryWrapper<SettingKeyEntity> wrapper = new LambdaQueryWrapper<>();
                    if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
                        wrapper.and(w -> w.like(SettingKeyEntity::getKey, q.getKeyword())
                                .or().like(SettingKeyEntity::getName, q.getKeyword())
                                .or().like(SettingKeyEntity::getRemark, q.getKeyword()));
                    }
                    return wrapper;
                },
                SortField.of("key", SortDirection.ASC), SortField.of("createTime", SortDirection.DESC));
    }

    private Optional<SettingKey> findByKey(String key) {
        return settingKeyDao.findByKey(key);
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
