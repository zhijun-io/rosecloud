package io.rosecloud.system.service.impl;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.service.TenantProvisioner;
import io.rosecloud.system.service.dto.TenantApplyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    TenantRepository tenantRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    TenantProvisioner tenantProvisioner;
    @Mock
    NoticePublishApi noticePublishApi;

    private TenantServiceImpl service() {
        return new TenantServiceImpl(tenantRepository, passwordEncoder, tenantProvisioner, noticePublishApi);
    }

    @Test
    void applyEncodesAdminPasswordAndSendsPendingNotice() {
        TenantApplyRequest request = new TenantApplyRequest("Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", "admin", "plain");
        when(passwordEncoder.encode("plain")).thenReturn("hashed");
        when(tenantRepository.insert(any(Tenant.class), any(), any())).thenReturn("tenant-99");
        when(noticePublishApi.publish(any(NoticePublishRequest.class)))
                .thenReturn(ApiResponse.ok(1L));

        String id = service().apply(request);

        assertEquals("tenant-99", id);
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).insert(tenantCaptor.capture(), any(), any());
        assertEquals(TenantStatus.PENDING, tenantCaptor.getValue().getStatus());
        ArgumentCaptor<NoticePublishRequest> noticeCaptor = ArgumentCaptor.forClass(NoticePublishRequest.class);
        verify(noticePublishApi).publish(noticeCaptor.capture());
        assertEquals(NoticeTargetType.ROLE.code(), noticeCaptor.getValue().targetType());
        assertEquals("platform-admin", noticeCaptor.getValue().targetRoleCode());
        assertEquals("新租户申请待审核", noticeCaptor.getValue().title());
    }

    @Test
    void enableRejectsExpiredTenant() {
        Tenant tenant = new Tenant("tenant-7", "Acme", TenantStatus.DISABLED,
                "Owner", "13800000000", LocalDate.now().minusDays(1), "remark", null);
        when(tenantRepository.findById("tenant-7")).thenReturn(Optional.of(tenant));

        BizException ex = assertThrows(BizException.class, () -> service().enable("tenant-7"));

        assertEquals("system.tenant_status_invalid", ex.getErrorCode().code());
        verify(tenantRepository, never()).updateStatus(any(), any());
    }

    @Test
    void getReturnsRepositoryValue() {
        Tenant tenant = new Tenant("tenant-8", "Beta", TenantStatus.ENABLED,
                "Owner", "13800000000", LocalDate.now().plusDays(1), "remark", null);
        when(tenantRepository.findById("tenant-8")).thenReturn(Optional.of(tenant));

        assertEquals(tenant, service().get("tenant-8"));
    }
}
