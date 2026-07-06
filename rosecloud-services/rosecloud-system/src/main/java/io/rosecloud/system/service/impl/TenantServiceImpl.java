package io.rosecloud.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.TaskTypes;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.TaskService;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.dto.TaskCreateRequest;
import io.rosecloud.system.service.dto.TenantApplyRequest;
import io.rosecloud.system.task.TenantProvisioningPayload;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public TenantServiceImpl(TenantRepository tenantRepository, PasswordEncoder passwordEncoder,
                             TaskService taskService, ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    @AuditLog(action = "tenant-apply", description = "申请租户")
    @Override
    public Long apply(TenantApplyRequest request) {
        if (tenantRepository.existsByCode(request.code())) {
            throw new BizException(SystemErrorCode.TENANT_CODE_EXISTS);
        }
        Tenant tenant = new Tenant(null, request.name(), request.code(), TenantStatus.PENDING,
                request.contactUser(), request.contactPhone(), request.expireTime(), request.remark());
        String passwordHash = request.adminPassword() == null || request.adminPassword().isBlank()
                ? null : passwordEncoder.encode(request.adminPassword());
        return tenantRepository.insert(tenant, request.adminUsername(), passwordHash);
    }

    /**
     * Dispatches tenant provisioning as an async task (main line of the task
     * center). Returns the task id so the caller can track progress; the tenant
     * stays {@link TenantStatus#PENDING} until provisioning succeeds, then the
     * {@link io.rosecloud.system.service.TenantProvisioner} enables it.
     */
    @AuditLog(action = "tenant-open", description = "开通租户")
    @Override
    public Long open(Long id) {
        Tenant tenant = load(id);
        if (tenant.status() != TenantStatus.PENDING) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        String payload = writePayload(new TenantProvisioningPayload(id));
        return taskService.create(new TaskCreateRequest("开通租户-" + tenant.name(),
                TaskTypes.TENANT_PROVISIONING, payload, id));
    }

    @AuditLog(action = "tenant-disable", description = "停用租户")
    @Override
    public void disable(Long id) {
        Tenant tenant = load(id);
        if (tenant.status() != TenantStatus.ENABLED) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        tenantRepository.updateStatus(id, TenantStatus.DISABLED);
    }

    @AuditLog(action = "tenant-enable", description = "启用租户")
    @Override
    public void enable(Long id) {
        Tenant tenant = load(id);
        if (tenant.status() != TenantStatus.DISABLED) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        tenantRepository.updateStatus(id, TenantStatus.ENABLED);
    }

    @Override
    public PageResult<Tenant> page(long current, long size, String keyword) {
        return tenantRepository.page(current, size, keyword);
    }

    private String writePayload(TenantProvisioningPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize task payload", e);
        }
    }

    private Tenant load(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_NOT_FOUND));
    }
}
