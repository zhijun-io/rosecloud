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
        mapper.insert(toPO(settingKey));
    }

    @Override
    public void update(SettingKey settingKey) {
        mapper.updateById(toPO(settingKey));
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
        return mapper.selectList(new LambdaQueryWrapper<SettingKeyPO>()
                .orderByAsc(SettingKeyPO::getSettingKey)).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<SettingKey> page(long current, long size, String keyword) {
        Page<SettingKeyPO> page = new Page<>(current, size);
        LambdaQueryWrapper<SettingKeyPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SettingKeyPO::getSettingKey, keyword)
                    .or().like(SettingKeyPO::getName, keyword)
                    .or().like(SettingKeyPO::getRemark, keyword));
        }
        wrapper.orderByAsc(SettingKeyPO::getSettingKey);
        IPage<SettingKeyPO> result = mapper.selectPage(page, wrapper);
        List<SettingKey> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private SettingKey toDomain(SettingKeyPO po) {
        return new SettingKey(po.getSettingKey(), po.getName(), po.getRemark(), po.getUpdatedAt(), po.getUpdatedBy());
    }

    private SettingKeyPO toPO(SettingKey settingKey) {
        SettingKeyPO po = new SettingKeyPO();
        po.setSettingKey(settingKey.key());
        po.setName(settingKey.name());
        po.setRemark(settingKey.remark());
        po.setUpdatedAt(settingKey.updatedAt());
        po.setUpdatedBy(settingKey.updatedBy());
        return po;
    }
}
