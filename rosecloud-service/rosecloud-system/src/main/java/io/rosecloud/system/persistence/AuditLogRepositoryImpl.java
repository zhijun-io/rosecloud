package io.rosecloud.system.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.common.core.model.SortDirection;
import io.rosecloud.common.core.model.SortField;
import io.rosecloud.common.core.model.TimePageQuery;
import io.rosecloud.starter.data.PagedResults;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.AuditLogQuery;
import io.rosecloud.system.persistence.AuditLogEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.Optional;

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
        return Optional.ofNullable(auditLogMapper.selectById(id)).map(AuditLogEntity::toData).orElse(null);
    }

    @Override
    public PagedData<AuditLog> page(TimePageQuery pageQuery, AuditLogQuery query) {
        return PagedResults.page(pageQuery, AuditLogEntity.class, auditLogMapper,
                q -> {
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
                    if (q.getStartTime() != null) {
                        wrapper.ge(AuditLogEntity::getCreateTime, q.getStartTime());
                    }
                    if (q.getEndTime() != null) {
                        wrapper.le(AuditLogEntity::getCreateTime, q.getEndTime());
                    }
                    return wrapper;
                },
                SortField.of("createTime", SortDirection.DESC));
    }
}
