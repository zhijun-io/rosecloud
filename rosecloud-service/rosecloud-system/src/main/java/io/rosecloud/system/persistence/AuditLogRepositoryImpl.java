package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Default {@link AuditLogRepository} backed by MyBatis-Plus. */
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogMapper auditLogMapper;

    public AuditLogRepositoryImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void save(AuditLogRequest request) {
        AuditLogEntity po = new AuditLogEntity();
        po.setAction(request.action());
        po.setDescription(request.description());
        po.setPrincipal(request.principal());
        po.setTenantId(request.tenantId());
        po.setTarget(request.target());
        po.setElapsedMillis(request.elapsedMillis());
        po.setSuccess(request.success() ? 1 : 0);
        po.setError(request.error());
        po.setEntityType(request.entityType());
        po.setEntityId(request.entityId());
        po.setIpAddress(request.ipAddress());
        po.setSeverity(request.severity());
        auditLogMapper.insert(po);
    }

    @Override
    public AuditLog findById(Long id) {
        return toDomain(auditLogMapper.selectById(id));
    }

    @Override
    public PageResult<AuditLog> page(long current, long size, AuditLogQuery query) {
        Page<AuditLogEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.tenantId() != null) {
            wrapper.eq(AuditLogEntity::getTenantId, query.tenantId());
        }
        if (query.action() != null && !query.action().isBlank()) {
            wrapper.eq(AuditLogEntity::getAction, query.action());
        }
        if (query.principal() != null && !query.principal().isBlank()) {
            wrapper.eq(AuditLogEntity::getPrincipal, query.principal());
        }
        if (query.success() != null) {
            wrapper.eq(AuditLogEntity::getSuccess, query.success() ? 1 : 0);
        }
        if (query.entityType() != null && !query.entityType().isBlank()) {
            wrapper.eq(AuditLogEntity::getEntityType, query.entityType());
        }
        if (query.startTime() != null) {
            wrapper.ge(AuditLogEntity::getCreateTime, query.startTime());
        }
        if (query.endTime() != null) {
            wrapper.le(AuditLogEntity::getCreateTime, query.endTime());
        }
        wrapper.orderByDesc(AuditLogEntity::getCreateTime);
        IPage<AuditLogEntity> result = auditLogMapper.selectPage(page, wrapper);
        List<AuditLog> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private AuditLog toDomain(AuditLogEntity po) {
        if (po == null) {
            return null;
        }
        return new AuditLog(po.getId(), po.getAction(), po.getDescription(), po.getPrincipal(),
                po.getTenantId(), po.getTarget(), po.getElapsedMillis() == null ? 0L : po.getElapsedMillis(),
                po.getSuccess() != null && po.getSuccess() == 1, po.getError(), po.getCreateTime(),
                po.getEntityType(), po.getEntityId(), po.getIpAddress(), po.getSeverity());
    }
}
