package io.rosecloud.system.persistence;

import io.rosecloud.starter.data.dao.MyBatisDao;
import java.util.Optional;
import io.rosecloud.system.domain.SettingKey;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

@Repository
public class SettingKeyDao extends MyBatisDao<SettingKey, Long, SettingKeyEntity> {

    // ==== 配置键查询 ====

    public Optional<SettingKey> findByKey(String key) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<SettingKeyEntity>()
                        .eq(SettingKeyEntity::getKey, key)))
                .map(SettingKeyEntity::toData);
    }

    public boolean existsByKey(String key) {
        return mapper.exists(new LambdaQueryWrapper<SettingKeyEntity>()
                .eq(SettingKeyEntity::getKey, key));
    }

    public SettingKeyDao(SettingKeyMapper settingKeyMapper) {
        super(settingKeyMapper, SettingKeyEntity.class);
    }

    @Override
    protected Long getId(SettingKey domain) {
        return domain.getId();
    }

    @Override
    protected SettingKeyEntity toEntity(SettingKey domain) {
        return new SettingKeyEntity().toEntity(domain);
    }
}
