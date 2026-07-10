package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantProfileRepository;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.TenantProvisioner;
import io.rosecloud.system.service.dto.TenantCreateRequest;
import io.rosecloud.system.service.dto.TenantUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
    TenantProfileRepository tenantProfileRepository;
    @Mock
    TenantProvisioner tenantProvisioner;

    private TenantServiceImpl service() {
        return new TenantServiceImpl(tenantRepository, tenantProfileRepository, tenantProvisioner);
    }

    @Test
    void createPersistsTenantAndTriggersProvisioning() {
        TenantCreateRequest request = new TenantCreateRequest("tenant1", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", null, "admin");
        when(tenantProfileRepository.defaultProfileId()).thenReturn("profile-default");
        when(tenantRepository.findById("TENANT1")).thenReturn(Optional.empty());
        when(tenantRepository.insert(any(Tenant.class), any())).thenReturn("tenant-99");

        String id = service().create(request);

        assertEquals("TENANT1", id);
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).insert(tenantCaptor.capture(), any());
        assertEquals("TENANT1", tenantCaptor.getValue().getId());
        assertEquals(TenantStatus.PENDING, tenantCaptor.getValue().getStatus());
        assertEquals("profile-default", tenantCaptor.getValue().getTenantProfileId());
        verify(tenantProvisioner).provision("TENANT1");
    }

    @Test
    void createUsesProvidedTenantProfile() {
        TenantCreateRequest request = new TenantCreateRequest("tenant2", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", "profile-custom", "admin");
        Tenant pending = new Tenant("TENANT2", "Acme", TenantStatus.PENDING,
                "Owner", "13800000000", LocalDate.now().plusDays(30), "remark", "profile-custom", null);
        when(tenantProfileRepository.findById("profile-custom")).thenReturn(Optional.of(
                new io.rosecloud.system.domain.TenantProfile("profile-custom", "Custom", "tier",
                        new io.rosecloud.system.domain.TenantProfileData("custom", 1, 1, 1, 1, java.util.List.of()))));
        when(tenantRepository.findById("TENANT2")).thenReturn(Optional.empty());
        when(tenantRepository.insert(any(Tenant.class), any())).thenReturn("TENANT2");
        when(tenantRepository.findById("TENANT2")).thenReturn(Optional.of(pending));

        assertEquals("TENANT2", service().create(request));
        verify(tenantProvisioner).provision("TENANT2");
    }

    @Test
    void createRejectsReservedSystemTenantId() {
        TenantCreateRequest request = new TenantCreateRequest("root", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", null, "admin");

        BizException ex = assertThrows(BizException.class, () -> service().create(request));

        assertEquals(SystemErrorCode.TENANT_ID_RESERVED, ex.getErrorCode());
        verify(tenantRepository, never()).insert(any(), any());
    }

    @Test
    void updateReusesExistingProfileWhenNotSpecified() {
        Tenant existing = new Tenant("tenant-200", "Old", TenantStatus.ENABLED,
                "Owner", "13800000000", LocalDate.now().plusDays(30), "remark", "profile-default", null);
        when(tenantRepository.findById("tenant-200")).thenReturn(Optional.of(existing));
        when(tenantProfileRepository.findById("profile-default")).thenReturn(Optional.of(
                new io.rosecloud.system.domain.TenantProfile("profile-default", "Default", "tier",
                        new io.rosecloud.system.domain.TenantProfileData("default", 1, 1, 1, 1,
                                java.util.List.of()))));

        service().update("tenant-200", new TenantUpdateRequest("New", "Owner-2", "13900000000",
                LocalDate.now().plusDays(60), "new remark", null));

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).update(tenantCaptor.capture());
        assertEquals("profile-default", tenantCaptor.getValue().getTenantProfileId());
        assertEquals("New", tenantCaptor.getValue().getName());
    }

    @Test
    void deleteDelegatesToRepository() {
        Tenant existing = new Tenant("tenant-300", "Old", TenantStatus.ENABLED,
                "Owner", "13800000000", LocalDate.now().plusDays(30), "remark", null, null);
        when(tenantRepository.findById("tenant-300")).thenReturn(Optional.of(existing));

        service().delete("tenant-300");

        verify(tenantRepository).deleteById("tenant-300");
    }

    @Test
    void openRejectsEnabledTenant() {
        Tenant tenant = new Tenant("tenant-400", "Acme", TenantStatus.ENABLED,
                "Owner", "13800000000", LocalDate.now().plusDays(30), "remark", null, null);
        when(tenantRepository.findById("tenant-400")).thenReturn(Optional.of(tenant));

        BizException ex = assertThrows(BizException.class, () -> service().open("tenant-400"));

        assertEquals(SystemErrorCode.TENANT_STATUS_INVALID, ex.getErrorCode());
        verify(tenantProvisioner, never()).provision(any());
    }

    @Test
    void openProvisionsPendingTenant() {
        Tenant tenant = new Tenant("tenant-401", "Acme", TenantStatus.PENDING,
                "Owner", "13800000000", LocalDate.now().plusDays(30), "remark", null, null);
        when(tenantRepository.findById("tenant-401")).thenReturn(Optional.of(tenant));

        assertEquals("tenant-401", service().open("tenant-401"));
        verify(tenantProvisioner).provision("tenant-401");
    }

    @Test
    void disableUpdatesEnabledTenantToDisabled() {
        Tenant tenant = new Tenant("tenant-500", "Acme", TenantStatus.ENABLED,
                "Owner", "13800000000", LocalDate.now().plusDays(30), "remark", null, null);
        when(tenantRepository.findById("tenant-500")).thenReturn(Optional.of(tenant));

        service().disable("tenant-500");

        verify(tenantRepository).updateStatus("tenant-500", TenantStatus.DISABLED);
    }

    @Test
    void enableUpdatesDisabledTenantToEnabled() {
        Tenant tenant = new Tenant("tenant-600", "Acme", TenantStatus.DISABLED,
                "Owner", "13800000000", LocalDate.now().plusDays(30), "remark", null, null);
        when(tenantRepository.findById("tenant-600")).thenReturn(Optional.of(tenant));

        service().enable("tenant-600");

        verify(tenantRepository).updateStatus("tenant-600", TenantStatus.ENABLED);
    }

    @Test
    void enableRejectsExpiredTenant() {
        Tenant tenant = new Tenant("tenant-7", "Acme", TenantStatus.DISABLED,
                "Owner", "13800000000", LocalDate.now().minusDays(1), "remark", null, null);
        when(tenantRepository.findById("tenant-7")).thenReturn(Optional.of(tenant));

        BizException ex = assertThrows(BizException.class, () -> service().enable("tenant-7"));

        assertEquals("system.tenant_status_invalid", ex.getErrorCode().code());
        verify(tenantRepository, never()).updateStatus(any(), any());
    }

    @Test
    void getReturnsRepositoryValue() {
        Tenant tenant = new Tenant("tenant-8", "Beta", TenantStatus.ENABLED,
                "Owner", "13800000000", LocalDate.now().plusDays(1), "remark", null, null);
        when(tenantRepository.findById("tenant-8")).thenReturn(Optional.of(tenant));

        assertEquals(tenant, service().get("tenant-8"));
    }
}
