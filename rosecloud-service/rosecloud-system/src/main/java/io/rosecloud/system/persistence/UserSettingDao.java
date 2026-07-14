package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.system.domain.UserSetting;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO for user settings. The {@link UserSetting} domain lacks an {@code id} field
 * (using {@code userId + key} as the natural key), so this DAO provides custom
 * query methods rather than extending {@code MyBatisDao}.
 */
@Repository
public class UserSettingDao {

    private final UserSettingMapper mapper;

    public UserSettingDao(UserSettingMapper mapper) {
        this.mapper = mapper;
    }

    public List<UserSetting> listByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<UserSettingEntity>()
                        .eq(UserSettingEntity::getUserId, userId)
                        .orderByAsc(UserSettingEntity::getSettingKey))
                .stream().map(UserSettingEntity::toData).toList();
    }

    public Optional<UserSetting> findByUserIdAndKey(Long userId, String key) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<UserSettingEntity>()
                        .eq(UserSettingEntity::getUserId, userId)
                        .eq(UserSettingEntity::getSettingKey, key)))
                .map(UserSettingEntity::toData);
    }

    public UserSettingEntity findEntityByUserIdAndKey(Long userId, String key) {
        return mapper.selectOne(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getUserId, userId)
                .eq(UserSettingEntity::getSettingKey, key));
    }

    public void insert(UserSettingEntity entity) {
        mapper.insert(entity);
    }

    public void updateById(UserSettingEntity entity) {
        mapper.updateById(entity);
    }

    public void deleteByUserIdAndKey(Long userId, String key) {
        mapper.delete(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getUserId, userId)
                .eq(UserSettingEntity::getSettingKey, key));
    }
}
