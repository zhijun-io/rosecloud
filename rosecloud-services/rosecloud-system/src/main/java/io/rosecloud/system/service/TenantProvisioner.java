package io.rosecloud.system.service;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.RoleRepository;
import io.rosecloud.system.domain.TenantAdminCredentials;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Provisions a tenant: creates its first admin (from credentials captured at
 * apply time), grants the tenant-admin role, clears the stored credentials and
 * enables the tenant. Extracted so it can run as an async task; idempotent via
 * cleared credentials (a no-op enable when credentials are already consumed).
 */
@Component
public class TenantProvisioner {

    /** Platform-level role granted to each tenant's first admin on open. */
    private static final String TENANT_ADMIN_ROLE_CODE = "tenant-admin";

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;

    public TenantProvisioner(TenantRepository tenantRepository, RoleRepository roleRepository,
                             UserService userService) {
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
    }

    @Transactional
    public void provision(Long tenantId) {
        TenantAdminCredentials creds = tenantRepository.findAdminCredentials(tenantId).orElse(null);
        if (creds == null || creds.username() == null || creds.username().isBlank()
                || creds.passwordHash() == null) {
            tenantRepository.updateStatus(tenantId, TenantStatus.ENABLED);
            return;
        }
        Role tenantAdminRole = roleRepository.findByCode(TENANT_ADMIN_ROLE_CODE)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
        Long userId = userService.createWithHash(creds.username(), creds.passwordHash(),
                creds.username(), tenantId);
        userService.assignRoles(userId, List.of(tenantAdminRole.id()));
        tenantRepository.clearAdminPassword(tenantId);
        tenantRepository.updateStatus(tenantId, TenantStatus.ENABLED);
    }
}
