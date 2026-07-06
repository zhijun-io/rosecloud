package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.RoleRepository;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantAdminCredentials;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.UserService;
import io.rosecloud.system.service.dto.TenantApplyRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantServiceImpl implements TenantService {

    /** Platform-level role granted to each tenant's first admin on open. */
    private static final String TENANT_ADMIN_ROLE_CODE = "tenant-admin";

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public TenantServiceImpl(TenantRepository tenantRepository, RoleRepository roleRepository,
                             UserService userService, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
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

    @AuditLog(action = "tenant-open", description = "开通租户")
    @Transactional
    @Override
    public void open(Long id) {
        Tenant tenant = load(id);
        if (tenant.status() != TenantStatus.PENDING) {
            throw new BizException(SystemErrorCode.TENANT_STATUS_INVALID);
        }
        provisionAdmin(id);
        tenantRepository.updateStatus(id, TenantStatus.ENABLED);
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

    private void provisionAdmin(Long tenantId) {
        TenantAdminCredentials creds = tenantRepository.findAdminCredentials(tenantId).orElse(null);
        if (creds == null || creds.username() == null || creds.username().isBlank()
                || creds.passwordHash() == null) {
            return;
        }
        Role tenantAdminRole = roleRepository.findByCode(TENANT_ADMIN_ROLE_CODE)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
        Long userId = userService.createWithHash(creds.username(), creds.passwordHash(),
                creds.username(), tenantId);
        userService.assignRoles(userId, List.of(tenantAdminRole.id()));
        tenantRepository.clearAdminPassword(tenantId);
    }

    private Tenant load(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new BizException(SystemErrorCode.TENANT_NOT_FOUND));
    }
}
