package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.rosecloud.starter.data.dao.MyBatisDao;
import io.rosecloud.system.domain.TenantProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TenantProfileDao extends MyBatisDao<TenantProfile, String, TenantProfileEntity> {

    public TenantProfileDao(TenantProfileMapper tenantProfileMapper) {
        super(tenantProfileMapper, TenantProfileEntity.class);
    }

    public List<TenantProfile> findAllOrdered() {
        return mapper.selectList(new LambdaQueryWrapper<TenantProfileEntity>()
                        .orderByDesc(TenantProfileEntity::getIsDefault)
                        .orderByAsc(TenantProfileEntity::getId))
                .stream().map(TenantProfileEntity::toData).toList();
    }

    public void makeDefault(String id) {
        mapper.update(null, new LambdaUpdateWrapper<TenantProfileEntity>()
                .setSql("is_default = CASE WHEN id = {0} THEN 1 ELSE 0 END", id));
    }

    public Optional<TenantProfile> findDefault() {
        return Optional.ofNullable(mapper.selectOne(
                        new LambdaQueryWrapper<TenantProfileEntity>().eq(TenantProfileEntity::getIsDefault, 1)))
                .map(TenantProfileEntity::toData);
    }

    @Override
    protected String getId(TenantProfile domain) {
        return domain.getId();
    }

    @Override
    protected TenantProfileEntity toEntity(TenantProfile domain) {
        return new TenantProfileEntity().toEntity(domain);
    }
}
