package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogMapper mapper;

    public AuditLogRepositoryImpl(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(AuditLog log) {
        AuditLogPO po = new AuditLogPO();
        po.setAction(log.action());
        po.setDescription(log.description());
        po.setPrincipal(log.principal());
        po.setTenantId(log.tenantId());
        po.setTarget(log.target());
        po.setElapsedMillis(log.elapsedMillis());
        po.setSuccess(log.success() ? 1 : 0);
        po.setError(log.error());
        mapper.insert(po);
    }

    @Override
    public PageResult<AuditLog> page(long current, long size, String action, String principal) {
        Page<AuditLogPO> page = new Page<>(current, size);
        LambdaQueryWrapper<AuditLogPO> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isBlank()) {
            wrapper.eq(AuditLogPO::getAction, action);
        }
        if (principal != null && !principal.isBlank()) {
            wrapper.eq(AuditLogPO::getPrincipal, principal);
        }
        wrapper.orderByDesc(AuditLogPO::getCreateTime);
        IPage<AuditLogPO> result = mapper.selectPage(page, wrapper);
        List<AuditLog> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private AuditLog toDomain(AuditLogPO po) {
        return new AuditLog(po.getId(), po.getAction(), po.getDescription(), po.getPrincipal(),
                po.getTenantId(), po.getTarget(), po.getElapsedMillis() == null ? 0L : po.getElapsedMillis(),
                po.getSuccess() != null && po.getSuccess() == 1, po.getError(), po.getCreateTime());
    }
}
