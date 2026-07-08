package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.TenantProvisioner;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.dto.TenantApplyRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantProvisioner tenantProvisioner;
    private final NoticePublishApi noticePublishApi;

    public TenantServiceImpl(TenantRepository tenantRepository, PasswordEncoder passwordEncoder,
                             TenantProvisioner tenantProvisioner, NoticePublishApi noticePublishApi) {
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantProvisioner = tenantProvisioner;
        this.noticePublishApi = noticePublishApi;
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
        Long id = tenantRepository.insert(tenant, request.adminUsername(), passwordHash);
        publishPlatformNotice("新租户申请待审核", request.name() + "（" + request.code() + "）已提交申请，等待审核。",
                NoticeTargetType.ROLE.code(), null, "platform-admin");
        return id;
    }

    @Override
    public Tenant get(Long id) {
        return load(id);
    }

    /**
     * Dispatches tenant provisioning asynchronously. Returns the tenant id so
     * the caller can track the tenant record; the tenant stays
     * {@link TenantStatus#PENDING} until provisioning succeeds.
     */
    @AuditLog(action = "tenant-open", description = "开通租户")
    @Override
    public Long open(Long id) {
        Tenant tenant = load(id);
        if (tenant.status() != TenantStatus.PENDING) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        tenantProvisioner.provision(id);
        return id;
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
        if (tenant.expireTime() != null && tenant.expireTime().isBefore(LocalDate.now())) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        if (tenant.status() != TenantStatus.DISABLED) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        tenantRepository.updateStatus(id, TenantStatus.ENABLED);
    }

    @Override
    public PageResult<Tenant> page(long current, long size, String keyword) {
        return tenantRepository.page(current, size, keyword);
    }

    private Tenant load(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_NOT_FOUND));
    }

    private void publishPlatformNotice(String title, String content, Integer targetType, Long targetTenantId,
                                       String targetRoleCode) {
        try {
            noticePublishApi.publish(new NoticePublishRequest(title, content, targetType, targetTenantId,
                    targetRoleCode, null, LocalDateTime.now(), null, null, false, null));
        } catch (Exception ignored) {
            // Notification is best-effort; lifecycle must not fail because of it.
        }
    }
}
