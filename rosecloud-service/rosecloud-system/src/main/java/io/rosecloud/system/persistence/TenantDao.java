package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.rosecloud.starter.data.dao.MyBatisDao;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.domain.Tenant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TenantDao extends MyBatisDao<Tenant, String, TenantEntity> {

    public TenantDao(TenantMapper tenantMapper) {
        super(tenantMapper, TenantEntity.class);
    }

    public List<String> findAllIds() {
        return mapper.selectList(new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getDeleted, 0)
                        .ne(TenantEntity::getId, TenantContextHolder.SYSTEM_TENANT_ID))
                .stream().map(TenantEntity::getId).toList();
    }

    public Tenant create(Tenant domain, String adminUsername) {
        TenantEntity entity = toEntity(domain);
        entity.setAdminUsername(adminUsername);
        mapper.insert(entity);
        return toDomain(entity);
    }

    public void updateStatus(String id, int statusCode) {
        mapper.update(null, new LambdaUpdateWrapper<TenantEntity>()
                .eq(TenantEntity::getId, id)
                .set(TenantEntity::getStatus, statusCode));
    }

    public long countByProfileId(String profileId) {
        return mapper.selectCount(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getTenantProfileId, profileId));
    }

    @Override
    protected String getId(Tenant domain) {
        return domain.getId();
    }

    @Override
    protected TenantEntity toEntity(Tenant domain) {
        return new TenantEntity().toEntity(domain);
    }
}
