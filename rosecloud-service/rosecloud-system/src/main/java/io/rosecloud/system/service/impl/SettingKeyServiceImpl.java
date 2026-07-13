package io.rosecloud.system.service.impl;
import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.starter.data.PagedResults;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SettingKeyMapper;
import io.rosecloud.system.persistence.SystemSettingMapper;
import io.rosecloud.system.persistence.UserSettingEntity;
import io.rosecloud.system.persistence.UserSettingMapper;
import io.rosecloud.system.service.SettingKeyService;
import io.rosecloud.system.service.dto.SettingKeyCreateRequest;
import io.rosecloud.system.service.dto.SettingKeyUpdateRequest;
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

    private final SettingKeyMapper settingKeyMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final UserSettingMapper userSettingMapper;
    @AuditLog(action = "setting-key-create", description = "创建配置键")
    @Override
    public String create(SettingKeyCreateRequest request) {
        if (settingKeyMapper.selectCount(new LambdaQueryWrapper<SettingKeyEntity>()
                .eq(SettingKeyEntity::getKey, request.key())) > 0) {
            throw new BizException(SystemErrorCode.SETTING_KEY_EXISTS);
        }
        Long userId = currentUserId();
        LocalDateTime now = now();
        settingKeyMapper.insert(new SettingKeyEntity().toEntity(new SettingKey(null, request.key(), request.name(), request.remark(),
                now, userId, now, userId)));
        return request.key();
    }

    @AuditLog(action = "setting-key-update", description = "修改配置键")
    @Override
    public void update(String key, SettingKeyUpdateRequest request) {
        SettingKey current = load(key);
        Long userId = currentUserId();
        SettingKeyEntity existing = settingKeyMapper.selectOne(new LambdaQueryWrapper<SettingKeyEntity>()
                .eq(SettingKeyEntity::getKey, key));
        if (existing == null) {
            return;
        }
        SettingKeyEntity entity = new SettingKeyEntity().toEntity(new SettingKey(current.getId(), key, request.name(), request.remark(),
                current.getCreateTime(), current.getCreateBy(), now(), userId));
        entity.setId(existing.getId());
        settingKeyMapper.updateById(entity);
    }

    @AuditLog(action = "setting-key-delete", description = "删除配置键")
    @Transactional
    @Override
    public void delete(String key) {
        load(key);
        systemSettingMapper.deleteById(key);
        userSettingMapper.delete(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getSettingKey, key));
        SettingKeyEntity current = settingKeyMapper.selectOne(new LambdaQueryWrapper<SettingKeyEntity>()
                .eq(SettingKeyEntity::getKey, key));
        if (current != null) {
            settingKeyMapper.deleteById(current.getId());
        }
    }

    @Override
    public SettingKey get(String key) {
        return load(key);
    }

    @Override
    public List<SettingKey> list() {
        return settingKeyMapper.selectList(new LambdaQueryWrapper<SettingKeyEntity>()
                        .orderByAsc(SettingKeyEntity::getKey)).stream().map(SettingKeyEntity::toData).toList();
    }

    @Override
    public PagedData<SettingKey> page(PageQuery pageQuery) {
        return PagedResults.page(pageQuery, SettingKeyEntity.class, settingKeyMapper,
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

    private SettingKey load(String key) {
        return findByKey(key)
                .orElseThrow(() -> new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND));
    }

    private Optional<SettingKey> findByKey(String key) {
        return Optional.ofNullable(settingKeyMapper.selectOne(new LambdaQueryWrapper<SettingKeyEntity>()
                        .eq(SettingKeyEntity::getKey, key)))
                .map(SettingKeyEntity::toData);
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
