package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogMapper mapper;

    public AuditLogRepositoryImpl(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(AuditLog log) {
        AuditLogEntity po = new AuditLogEntity();
        po.setAction(log.getAction());
        po.setDescription(log.getDescription());
        po.setPrincipal(log.getPrincipal());
        po.setTenantId(log.getTenantId());
        po.setTarget(log.getTarget());
        po.setElapsedMillis(log.getElapsedMillis());
        po.setSuccess(log.isSuccess() ? 1 : 0);
        po.setError(log.getError());
        mapper.insert(po);
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<AuditLog> page(long current, long size, String tenantId, String action, String principal) {
        Page<AuditLogEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(AuditLogEntity::getTenantId, tenantId);
        }
        if (action != null && !action.isBlank()) {
            wrapper.eq(AuditLogEntity::getAction, action);
        }
        if (principal != null && !principal.isBlank()) {
            wrapper.eq(AuditLogEntity::getPrincipal, principal);
        }
        wrapper.orderByDesc(AuditLogEntity::getCreateTime);
        IPage<AuditLogEntity> result = mapper.selectPage(page, wrapper);
        List<AuditLog> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private AuditLog toDomain(AuditLogEntity po) {
        return new AuditLog(po.getId(), po.getAction(), po.getDescription(), po.getPrincipal(),
                po.getTenantId(), po.getTarget(), po.getElapsedMillis() == null ? 0L : po.getElapsedMillis(),
                po.getSuccess() != null && po.getSuccess() == 1, po.getError(), po.getCreateTime());
    }
}
