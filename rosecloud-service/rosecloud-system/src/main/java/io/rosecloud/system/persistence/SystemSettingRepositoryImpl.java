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
        return mapper.selectList(new LambdaQueryWrapper<SystemSettingEntity>()
                .orderByAsc(SystemSettingEntity::getSettingKey)).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(SystemSetting setting) {
        SystemSettingEntity existing = mapper.selectById(setting.getKey());
        SystemSettingEntity po = toEntity(setting);
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

    private SystemSetting toDomain(SystemSettingEntity po) {
        return new SystemSetting(po.getSettingKey(), po.getValue(), po.getUpdatedAt(), po.getUpdatedBy());
    }

    private SystemSettingEntity toEntity(SystemSetting setting) {
        SystemSettingEntity po = new SystemSettingEntity();
        po.setSettingKey(setting.getKey());
        po.setValue(setting.getValue());
        po.setUpdatedAt(setting.getUpdatedAt());
        po.setUpdatedBy(setting.getUpdatedBy());
        return po;
    }
}
