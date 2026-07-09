package io.rosecloud.system.service;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.RoleRepository;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.UserActivationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provisions a tenant: creates its first admin email account, grants the tenant-admin
 * role, and enables the tenant. The password is set later through the
 * authentication flow instead of being stored on the tenant record.
 */
@Component
public class TenantProvisioner {

    /** Platform-level role granted to each tenant's first admin on open. */
    private static final String TENANT_ADMIN_ROLE_CODE = "tenant-admin";

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final UserActivationService userActivationService;
    private final UserRepository userRepository;
    private final NoticePublishApi noticePublishApi;

    public TenantProvisioner(TenantRepository tenantRepository, RoleRepository roleRepository,
                             UserService userService, UserActivationService userActivationService,
                             UserRepository userRepository,
                             NoticePublishApi noticePublishApi) {
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.userActivationService = userActivationService;
        this.userRepository = userRepository;
        this.noticePublishApi = noticePublishApi;
    }

    @Async("tenantProvisioningExecutor")
    @Transactional
    public void provision(String tenantId) {
        String adminUsername = tenantRepository.findAdminUsername(tenantId).orElse(null);
        if (adminUsername == null || adminUsername.isBlank()) {
            tenantRepository.updateStatus(tenantId, TenantStatus.ENABLED);
            publishTenantNotice(tenantId, "租户已开通", "租户已完成开通。");
            return;
        }
        Role tenantAdminRole = roleRepository.findByCode(TENANT_ADMIN_ROLE_CODE)
                .orElseThrow(() -> new BizException(SystemErrorCode.ROLE_NOT_FOUND));
        Long userId = userService.createWithoutPassword(adminUsername, adminUsername, tenantId);
        userService.assignRoles(userId, List.of(tenantAdminRole.getId()));
        userActivationService.resend(adminUsername);
        tenantRepository.updateStatus(tenantId, TenantStatus.ENABLED);
        publishTenantNotice(tenantId, "租户已开通", "租户已完成开通，首个管理员账号已初始化。");
    }

    private void publishTenantNotice(String tenantId, String title, String content) {
        try {
            List<NoticeRecipient> recipients = userRepository.findContacts(
                    NoticeTargetType.TENANT.code(), tenantId, null, null);
            noticePublishApi.publish(new NoticePublishRequest(title, content, NoticeTargetType.TENANT.code(),
                    tenantId, null, null, null, LocalDateTime.now(), null, null, false, null, recipients));
        } catch (Exception ignored) {
            // Best-effort: provisioning must not fail because notice delivery is unavailable.
        }
    }
}
