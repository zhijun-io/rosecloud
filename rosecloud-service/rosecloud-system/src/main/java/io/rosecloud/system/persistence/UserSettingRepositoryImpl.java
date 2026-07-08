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
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<UserSettingPO>()
                        .eq(UserSettingPO::getUserId, userId)
                        .eq(UserSettingPO::getSettingKey, key)))
                .map(this::toDomain);
    }

    @Override
    public List<UserSetting> findByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<UserSettingPO>()
                .eq(UserSettingPO::getUserId, userId)
                .orderByAsc(UserSettingPO::getSettingKey)).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(UserSetting setting) {
        UserSettingPO existing = mapper.selectOne(new LambdaQueryWrapper<UserSettingPO>()
                .eq(UserSettingPO::getUserId, setting.userId())
                .eq(UserSettingPO::getSettingKey, setting.key()));
        UserSettingPO po = toPO(setting);
        if (existing == null) {
            mapper.insert(po);
            return;
        }
        po.setId(existing.getId());
        mapper.updateById(po);
    }

    @Override
    public void deleteByUserIdAndKey(Long userId, String key) {
        mapper.delete(new LambdaQueryWrapper<UserSettingPO>()
                .eq(UserSettingPO::getUserId, userId)
                .eq(UserSettingPO::getSettingKey, key));
    }

    @Override
    public void deleteByKey(String key) {
        mapper.delete(new LambdaQueryWrapper<UserSettingPO>()
                .eq(UserSettingPO::getSettingKey, key));
    }

    private UserSetting toDomain(UserSettingPO po) {
        return new UserSetting(po.getUserId(), po.getSettingKey(), po.getValue(), po.getUpdatedAt(), po.getUpdatedBy());
    }

    private UserSettingPO toPO(UserSetting setting) {
        UserSettingPO po = new UserSettingPO();
        po.setUserId(setting.userId());
        po.setSettingKey(setting.key());
        po.setValue(setting.value());
        po.setUpdatedAt(setting.updatedAt());
        po.setUpdatedBy(setting.updatedBy());
        return po;
    }
}
