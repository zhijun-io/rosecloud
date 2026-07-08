package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.SettingKey;
import io.rosecloud.system.domain.SettingKeyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SettingKeyRepositoryImpl implements SettingKeyRepository {

    private final SettingKeyMapper mapper;

    public SettingKeyRepositoryImpl(SettingKeyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean existsByKey(String key) {
        return mapper.selectById(key) != null;
    }

    @Override
    public void insert(SettingKey settingKey) {
        mapper.insert(toEntity(settingKey));
    }

    @Override
    public void update(SettingKey settingKey) {
        mapper.updateById(toEntity(settingKey));
    }

    @Override
    public Optional<SettingKey> findByKey(String key) {
        return Optional.ofNullable(mapper.selectById(key))
                .map(this::toDomain);
    }

    @Override
    public void deleteByKey(String key) {
        mapper.deleteById(key);
    }

    @Override
    public List<SettingKey> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<SettingKeyEntity>()
                .orderByAsc(SettingKeyEntity::getSettingKey)).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<SettingKey> page(long current, long size, String keyword) {
        Page<SettingKeyEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<SettingKeyEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SettingKeyEntity::getSettingKey, keyword)
                    .or().like(SettingKeyEntity::getName, keyword)
                    .or().like(SettingKeyEntity::getRemark, keyword));
        }
        wrapper.orderByAsc(SettingKeyEntity::getSettingKey);
        IPage<SettingKeyEntity> result = mapper.selectPage(page, wrapper);
        List<SettingKey> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private SettingKey toDomain(SettingKeyEntity po) {
        return new SettingKey(po.getSettingKey(), po.getName(), po.getRemark(), po.getUpdatedAt(), po.getUpdatedBy());
    }

    private SettingKeyEntity toEntity(SettingKey settingKey) {
        SettingKeyEntity po = new SettingKeyEntity();
        po.setSettingKey(settingKey.getKey());
        po.setName(settingKey.getName());
        po.setRemark(settingKey.getRemark());
        po.setUpdatedAt(settingKey.getUpdatedAt());
        po.setUpdatedBy(settingKey.getUpdatedBy());
        return po;
    }
}
