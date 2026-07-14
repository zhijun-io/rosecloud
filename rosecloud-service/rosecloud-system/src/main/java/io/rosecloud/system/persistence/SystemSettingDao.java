package io.rosecloud.system.persistence;

    import io.rosecloud.starter.data.dao.MyBatisDao;
    import java.util.List;
    import io.rosecloud.system.domain.SystemSetting;
    import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
    import org.springframework.stereotype.Repository;

@Repository
public class SystemSettingDao extends MyBatisDao<SystemSetting, String, SystemSettingEntity> {

    // ==== 配置项查询 ====

    public List<SystemSetting> findAllOrderByKey() {
        return mapper.selectList(new LambdaQueryWrapper<SystemSettingEntity>()
                        .orderByAsc(SystemSettingEntity::getSettingKey))
                .stream().map(SystemSettingEntity::toData).toList();
    }

    /**
     * Insert or update a system setting by its string key.
     * <p>Overrides the base save()-which always calls updateById when
     * the ID is non-null-with an explicit existence check so that new
     * settings with a known key string still get inserted.
     */
    @Override
    public SystemSetting save(SystemSetting domain) {
        SystemSettingEntity entity = toEntity(domain);
        SystemSettingEntity existing = mapper.selectById(getId(domain));
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity.toData();
    }

    public SystemSettingDao(SystemSettingMapper systemSettingMapper) {
        super(systemSettingMapper, SystemSettingEntity.class);
    }

    @Override
    protected String getId(SystemSetting domain) {
        return domain.getKey();
    }

    @Override
    protected SystemSettingEntity toEntity(SystemSetting domain) {
        return new SystemSettingEntity().toEntity(domain);
    }
}
