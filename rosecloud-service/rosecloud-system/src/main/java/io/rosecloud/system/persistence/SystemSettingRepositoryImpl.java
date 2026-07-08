package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.system.domain.SystemSetting;
import io.rosecloud.system.domain.SystemSettingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SystemSettingRepositoryImpl implements SystemSettingRepository {

    private final SystemSettingMapper mapper;

    public SystemSettingRepositoryImpl(SystemSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<SystemSetting> findByKey(String key) {
        return Optional.ofNullable(mapper.selectById(key))
                .map(this::toDomain);
    }

    @Override
    public List<SystemSetting> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<SystemSettingPO>()
                .orderByAsc(SystemSettingPO::getSettingKey)).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(SystemSetting setting) {
        SystemSettingPO existing = mapper.selectById(setting.key());
        SystemSettingPO po = toPO(setting);
        if (existing == null) {
            mapper.insert(po);
            return;
        }
        mapper.updateById(po);
    }

    @Override
    public void deleteByKey(String key) {
        mapper.deleteById(key);
    }

    private SystemSetting toDomain(SystemSettingPO po) {
        return new SystemSetting(po.getSettingKey(), po.getValue(), po.getUpdatedAt(), po.getUpdatedBy());
    }

    private SystemSettingPO toPO(SystemSetting setting) {
        SystemSettingPO po = new SystemSettingPO();
        po.setSettingKey(setting.key());
        po.setValue(setting.value());
        po.setUpdatedAt(setting.updatedAt());
        po.setUpdatedBy(setting.updatedBy());
        return po;
    }
}
