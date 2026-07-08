package io.rosecloud.system.service;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.RoleRepository;
import io.rosecloud.system.domain.TenantAdminCredentials;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provisions a tenant: creates its first admin (from credentials captured at
 * apply time), grants the tenant-admin role, clears the stored credentials and
 * enables the tenant. Extracted so it can run asynchronously; idempotent via
 * cleared credentials (a no-op enable when credentials are already consumed).
 */
@Component
public class TenantProvisioner {

    /** Platform-level role granted to each tenant's first admin on open. */
    private static final String TENANT_ADMIN_ROLE_CODE = "tenant-admin";

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final NoticePublishApi noticePublishApi;

    public TenantProvisioner(TenantRepository tenantRepository, RoleRepository roleRepository,
                             UserService userService, NoticePublishApi noticePublishApi) {
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.noticePublishApi = noticePublishApi;
    }

    @Async("tenantProvisioningExecutor")
    @Transactional
    public void provision(Long tenantId) {
        TenantAdminCredentials creds = tenantRepository.findAdminCredentials(tenantId).orElse(null);
        if (creds == null || creds.getUsername() == null || creds.getUsername().isBlank()
                || creds.getPasswordHash() == null) {
            tenantRepository.updateStatus(tenantId, TenantStatus.ENABLED);
            publishTenantNotice(tenantId, "租户已开通", "租户已完成开通。");
            return;
        }
        Role tenantAdminRole = roleRepository.findByCode(TENANT_ADMIN_ROLE_CODE)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
        Long userId = userService.createWithHash(creds.getUsername(), creds.getPasswordHash(),
                creds.getUsername(), tenantId);
        userService.assignRoles(userId, List.of(tenantAdminRole.getId()));
        tenantRepository.clearAdminPassword(tenantId);
        tenantRepository.updateStatus(tenantId, TenantStatus.ENABLED);
        publishTenantNotice(tenantId, "租户已开通", "租户已完成开通，首个管理员账号已初始化。");
    }

    private void publishTenantNotice(Long tenantId, String title, String content) {
        try {
            noticePublishApi.publish(new NoticePublishRequest(title, content, NoticeTargetType.TENANT.code(),
                    tenantId, null, null, LocalDateTime.now(), null, null, false, null));
        } catch (Exception ignored) {
            // Best-effort: provisioning must not fail because notice delivery is unavailable.
        }
    }
}
