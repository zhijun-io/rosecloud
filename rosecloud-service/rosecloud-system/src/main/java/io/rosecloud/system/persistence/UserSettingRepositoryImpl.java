package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.system.domain.UserSetting;
import io.rosecloud.system.domain.UserSettingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserSettingRepositoryImpl implements UserSettingRepository {

    private final UserSettingMapper mapper;

    public UserSettingRepositoryImpl(UserSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserSetting> findByUserIdAndKey(Long userId, String key) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<UserSettingEntity>()
                        .eq(UserSettingEntity::getUserId, userId)
                        .eq(UserSettingEntity::getSettingKey, key)))
                .map(this::toDomain);
    }

    @Override
    public List<UserSetting> findByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getUserId, userId)
                .orderByAsc(UserSettingEntity::getSettingKey)).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(UserSetting setting) {
        UserSettingEntity existing = mapper.selectOne(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getUserId, setting.getUserId())
                .eq(UserSettingEntity::getSettingKey, setting.getKey()));
        UserSettingEntity po = toEntity(setting);
        if (existing == null) {
            mapper.insert(po);
            return;
        }
        po.setId(existing.getId());
        mapper.updateById(po);
    }

    @Override
    public void deleteByUserIdAndKey(Long userId, String key) {
        mapper.delete(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getUserId, userId)
                .eq(UserSettingEntity::getSettingKey, key));
    }

    @Override
    public void deleteByKey(String key) {
        mapper.delete(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getSettingKey, key));
    }

    private UserSetting toDomain(UserSettingEntity po) {
        return new UserSetting(po.getUserId(), po.getSettingKey(), po.getValue(), po.getUpdatedAt(), po.getUpdatedBy());
    }

    private UserSettingEntity toEntity(UserSetting setting) {
        UserSettingEntity po = new UserSettingEntity();
        po.setUserId(setting.getUserId());
        po.setSettingKey(setting.getKey());
        po.setValue(setting.getValue());
        po.setUpdatedAt(setting.getUpdatedAt());
        po.setUpdatedBy(setting.getUpdatedBy());
        return po;
    }
}
