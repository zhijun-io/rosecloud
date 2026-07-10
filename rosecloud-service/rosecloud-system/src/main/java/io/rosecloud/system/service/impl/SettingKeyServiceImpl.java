package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.SettingKeyEntity;
import io.rosecloud.system.persistence.SettingKeyMapper;
import io.rosecloud.system.persistence.SystemSettingEntity;
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

@Service
public class SettingKeyServiceImpl implements SettingKeyService {

    private final SettingKeyMapper settingKeyMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final UserSettingMapper userSettingMapper;

    public SettingKeyServiceImpl(SettingKeyMapper settingKeyMapper,
                                SystemSettingMapper systemSettingMapper,
                                UserSettingMapper userSettingMapper) {
        this.settingKeyMapper = settingKeyMapper;
        this.systemSettingMapper = systemSettingMapper;
        this.userSettingMapper = userSettingMapper;
    }

    @AuditLog(action = "setting-key-create", description = "创建配置键")
    @Override
    public String create(SettingKeyCreateRequest request) {
        if (settingKeyMapper.selectCount(new LambdaQueryWrapper<SettingKeyEntity>()
                .eq(SettingKeyEntity::getKey, request.key())) > 0) {
            throw new BizException(SystemErrorCode.SETTING_KEY_EXISTS);
        }
        Long userId = currentUserId();
        LocalDateTime now = now();
        settingKeyMapper.insert(toEntity(new SettingKey(null, request.key(), request.name(), request.remark(),
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
        SettingKeyEntity entity = toEntity(new SettingKey(current.getId(), key, request.name(), request.remark(),
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
                        .orderByAsc(SettingKeyEntity::getKey)).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<SettingKey> page(long current, long size, String keyword) {
        Page<SettingKeyEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<SettingKeyEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SettingKeyEntity::getKey, keyword)
                    .or().like(SettingKeyEntity::getName, keyword)
                    .or().like(SettingKeyEntity::getRemark, keyword));
        }
        wrapper.orderByAsc(SettingKeyEntity::getKey);
        IPage<SettingKeyEntity> result = settingKeyMapper.selectPage(page, wrapper);
        List<SettingKey> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private SettingKey load(String key) {
        return findByKey(key)
                .orElseThrow(() -> new BizException(SystemErrorCode.SETTING_KEY_NOT_FOUND));
    }

    private Optional<SettingKey> findByKey(String key) {
        return Optional.ofNullable(settingKeyMapper.selectOne(new LambdaQueryWrapper<SettingKeyEntity>()
                        .eq(SettingKeyEntity::getKey, key)))
                .map(this::toDomain);
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

    private SettingKey toDomain(SettingKeyEntity po) {
        return new SettingKey(po.getId(), po.getKey(), po.getName(), po.getRemark(),
                po.getCreateTime(), po.getCreateBy(), po.getUpdateTime(), po.getUpdateBy());
    }

    private SettingKeyEntity toEntity(SettingKey settingKey) {
        SettingKeyEntity po = new SettingKeyEntity();
        po.setId(settingKey.getId());
        po.setKey(settingKey.getKey());
        po.setName(settingKey.getName());
        po.setRemark(settingKey.getRemark());
        po.setCreateTime(settingKey.getCreateTime());
        po.setCreateBy(settingKey.getCreateBy());
        po.setUpdateTime(settingKey.getUpdateTime());
        po.setUpdateBy(settingKey.getUpdateBy());
        return po;
    }
}
