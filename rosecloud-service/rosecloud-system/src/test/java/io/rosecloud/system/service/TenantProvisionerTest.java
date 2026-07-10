package io.rosecloud.system.service;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.system.service.dto.UserActivationInfo;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.Role;
import io.rosecloud.system.domain.RoleRepository;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProvisionerTest {

    @Mock
    TenantRepository tenantRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    UserService userService;
    @Mock
    UserActivationService userActivationService;
    @Mock
    NoticePublishApi noticePublishApi;

    private TenantProvisioner service() {
        return new TenantProvisioner(tenantRepository, roleRepository, userService, userActivationService,
                noticePublishApi);
    }

    @Test
    void provisionEnablesTenantWithoutAdminUsername() {
        when(tenantRepository.findAdminUsername("TENANT1")).thenReturn(Optional.of(" "));
        when(noticePublishApi.publish(any(NoticePublishRequest.class))).thenReturn(1L);

        service().provision("TENANT1");

        verify(tenantRepository).updateStatus("TENANT1", TenantStatus.ENABLED);
        verify(userService, never()).createWithoutPassword(any(), any(), any());
        verify(userActivationService, never()).resend(any());
    }

    @Test
    void provisionCreatesAdminAndAssignsRolesWhenUsernameExists() {
        when(tenantRepository.findAdminUsername("TENANT2")).thenReturn(Optional.of("admin"));
        when(roleRepository.findByCode("tenant-admin")).thenReturn(Optional.of(new Role(7L, "tenant-admin",
                "Tenant admin")));
        when(userService.createWithoutPassword("admin", "admin", "TENANT2")).thenReturn(88L);
        doNothing().when(userActivationService).resend("admin");
        when(noticePublishApi.publish(any(NoticePublishRequest.class))).thenReturn(1L);

        service().provision("TENANT2");

        verify(userService).createWithoutPassword("admin", "admin", "TENANT2");
        verify(userService).assignRoles(88L, java.util.List.of(7L));
        verify(userActivationService).resend("admin");
        verify(tenantRepository).updateStatus("TENANT2", TenantStatus.ENABLED);
        ArgumentCaptor<NoticePublishRequest> noticeCaptor = ArgumentCaptor.forClass(NoticePublishRequest.class);
        verify(noticePublishApi).publish(noticeCaptor.capture());
        assertEquals(NoticeTargetType.TENANT.code(), noticeCaptor.getValue().targetType());
        assertEquals("TENANT2", noticeCaptor.getValue().targetTenantId());
        assertEquals(null, noticeCaptor.getValue().targetRoleCode());
        assertEquals(null, noticeCaptor.getValue().targetUsername());
    }

    @Test
    void provisionFailsWhenTenantAdminRoleMissing() {
        when(tenantRepository.findAdminUsername("TENANT3")).thenReturn(Optional.of("admin"));
        when(roleRepository.findByCode("tenant-admin")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service().provision("TENANT3"));

        assertEquals(SystemErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
        verify(tenantRepository, never()).updateStatus(any(), any());
        verify(userService, never()).createWithoutPassword(any(), any(), any());
    }
}
