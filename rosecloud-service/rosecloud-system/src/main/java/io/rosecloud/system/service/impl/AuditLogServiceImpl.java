package io.rosecloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.api.audit.AuditLogApi;
import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.AuditLogEntity;
import io.rosecloud.system.persistence.AuditLogMapper;
import io.rosecloud.system.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuditLogServiceImpl implements AuditLogService, AuditLogApi {

    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public AuditLog get(Long id) {
        return findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.AUDIT_LOG_NOT_FOUND));
    }

    @Override
    public PageResult<AuditLog> page(long current, long size, String action, String username) {
        Page<AuditLogEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (TenantContextHolder.getTenantId() != null) {
            wrapper.eq(AuditLogEntity::getTenantId, TenantContextHolder.getTenantId());
        }
        if (action != null && !action.isBlank()) {
            wrapper.eq(AuditLogEntity::getAction, action);
        }
        if (username != null && !username.isBlank()) {
            wrapper.eq(AuditLogEntity::getPrincipal, username);
        }
        wrapper.orderByDesc(AuditLogEntity::getCreateTime);
        IPage<AuditLogEntity> result = auditLogMapper.selectPage(page, wrapper);
        List<AuditLog> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Optional<AuditLog> findById(Long id) {
        return Optional.ofNullable(auditLogMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public void save(AuditLogRequest auditLogRequest) {
        AuditLogEntity po = new AuditLogEntity();
        po.setAction(auditLogRequest.action());
        po.setDescription(auditLogRequest.description());
        po.setPrincipal(auditLogRequest.principal());
        po.setTenantId(auditLogRequest.tenantId());
        po.setTarget(auditLogRequest.target());
        po.setElapsedMillis(auditLogRequest.elapsedMillis());
        po.setSuccess(auditLogRequest.success() ? 1 : 0);
        po.setError(auditLogRequest.error());
        auditLogMapper.insert(po);
    }

    private AuditLog toDomain(AuditLogEntity po) {
        return new AuditLog(po.getId(), po.getAction(), po.getDescription(), po.getPrincipal(),
                po.getTenantId(), po.getTarget(), po.getElapsedMillis() == null ? 0L : po.getElapsedMillis(),
                po.getSuccess() != null && po.getSuccess() == 1, po.getError(), po.getCreateTime());
    }
}
