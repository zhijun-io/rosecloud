package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantMapper mapper;

    public TenantRepositoryImpl(TenantMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return mapper.exists(new LambdaQueryWrapper<TenantPO>().eq(TenantPO::getCode, code));
    }

    @Override
    public Long insert(Tenant tenant) {
        TenantPO po = toPO(tenant);
        po.setId(null);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void updateStatus(Long id, TenantStatus status) {
        mapper.update(null, new LambdaUpdateWrapper<TenantPO>()
                .eq(TenantPO::getId, id)
                .set(TenantPO::getStatus, status.code()));
    }

    @Override
    public PageResult<Tenant> page(long current, long size, String keyword) {
        Page<TenantPO> page = new Page<>(current, size);
        LambdaQueryWrapper<TenantPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(TenantPO::getName, keyword).or().like(TenantPO::getCode, keyword);
        }
        wrapper.orderByDesc(TenantPO::getCreateTime);
        IPage<TenantPO> result = mapper.selectPage(page, wrapper);
        List<Tenant> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Tenant toDomain(TenantPO po) {
        return new Tenant(po.getId(), po.getName(), po.getCode(),
                TenantStatus.of(po.getStatus()), po.getContactUser(), po.getContactPhone(),
                po.getExpireTime(), po.getRemark());
    }

    private TenantPO toPO(Tenant t) {
        TenantPO po = new TenantPO();
        po.setId(t.id());
        po.setName(t.name());
        po.setCode(t.code());
        po.setStatus(t.status() == null ? null : t.status().code());
        po.setContactUser(t.contactUser());
        po.setContactPhone(t.contactPhone());
        po.setExpireTime(t.expireTime());
        po.setRemark(t.remark());
        return po;
    }
}
