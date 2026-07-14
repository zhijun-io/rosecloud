package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantDao;
import io.rosecloud.system.persistence.TenantProfileDao;
import io.rosecloud.system.service.TenantProvisioner;
import io.rosecloud.system.service.dto.TenantCreateRequest;
import io.rosecloud.system.service.dto.TenantUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    TenantDao tenantDao;
    @Mock
    TenantProfileDao tenantProfileDao;
    @Mock
    TenantProvisioner tenantProvisioner;
    @Mock
    EntityCache<String, Tenant> tenantCache;
    @Mock
    EntityEventPublisher eventPublisher;

    @Captor
    ArgumentCaptor<Tenant> tenantCaptor;

    private TenantServiceImpl service() {
        return new TenantServiceImpl(tenantDao, tenantProfileDao, tenantProvisioner, tenantCache, eventPublisher);
    }

    @Test
    void createPersistsTenantAndTriggersProvisioning() {
        TenantCreateRequest request = new TenantCreateRequest("tenant1", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", null, "admin");
        when(tenantProfileDao.findDefault()).thenReturn(Optional.of(
                new TenantProfile("profile-default", "Default", "Default tier", true, null)));
        when(tenantCache.getOrLoadTransactional(eq("TENANT1"), any())).thenReturn(null,
                new Tenant("TENANT1", "Acme", TenantStatus.PENDING, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", "profile-default", null));

        String id = service().create(request);

        assertEquals("TENANT1", id);
        verify(tenantDao).create(tenantCaptor.capture(), eq("admin"));
        assertEquals("TENANT1", tenantCaptor.getValue().getId());
        assertEquals(TenantStatus.PENDING.code(), tenantCaptor.getValue().getStatus().code());
        assertEquals("profile-default", tenantCaptor.getValue().getTenantProfileId());
        verify(tenantProvisioner).provision("TENANT1");
    }

    @Test
    void createUsesProvidedTenantProfile() {
        TenantCreateRequest request = new TenantCreateRequest("tenant2", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", "profile-custom", "admin");
        when(tenantProfileDao.findById("profile-custom")).thenReturn(Optional.of(
                new TenantProfile("profile-custom", "Custom", "Custom tier", false, null)));
        when(tenantCache.getOrLoadTransactional(eq("TENANT2"), any())).thenReturn(null,
                new Tenant("TENANT2", "Acme", TenantStatus.PENDING, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", "profile-custom", null));

        assertEquals("TENANT2", service().create(request));
        verify(tenantProvisioner).provision("TENANT2");
    }

    @Test
    void createRejectsReservedSystemTenantId() {
        TenantCreateRequest request = new TenantCreateRequest("root", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", null, "admin");

        BizException ex = assertThrows(BizException.class, () -> service().create(request));

        assertEquals(SystemErrorCode.TENANT_ID_RESERVED, ex.getErrorCode());
        verify(tenantDao, never()).create(any(), any());
    }

    @Test
    void createRejectsDuplicateTenantId() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT9"), any())).thenReturn(
                new Tenant("TENANT9", "Acme", TenantStatus.ENABLED, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", null, null));
        TenantCreateRequest request = new TenantCreateRequest("tenant9", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", null, "admin");

        BizException ex = assertThrows(BizException.class, () -> service().create(request));

        assertEquals(SystemErrorCode.TENANT_CODE_EXISTS, ex.getErrorCode());
        verify(tenantDao, never()).create(any(), any());
    }

    @Test
    void updateReusesExistingProfileWhenNotSpecified() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT200"), any())).thenReturn(
                new Tenant("TENANT200", "Acme", TenantStatus.ENABLED, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", "profile-default", null));
        when(tenantProfileDao.findById("profile-default")).thenReturn(Optional.of(
                new TenantProfile("profile-default", "Default", "Default tier", true, null)));

        service().update("tenant200", new TenantUpdateRequest("New", "Owner-2", "13900000000",
                LocalDate.now().plusDays(60), "new remark", null));

        verify(tenantDao).save(any());
    }

    @Test
    void deleteDelegatesToMapper() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT300"), any())).thenReturn(
                new Tenant("TENANT300", "Acme", TenantStatus.ENABLED, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", null, null));

        service().delete("tenant300");

        verify(tenantProvisioner).deprovision("TENANT300");
        verify(tenantDao).removeById("TENANT300");
    }

    @Test
    void openRejectsEnabledTenant() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT400"), any())).thenReturn(
                new Tenant("TENANT400", "Acme", TenantStatus.ENABLED, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", null, null));

        BizException ex = assertThrows(BizException.class, () -> service().open("tenant400"));

        assertEquals(SystemErrorCode.TENANT_STATUS_INVALID, ex.getErrorCode());
        verify(tenantProvisioner, never()).provision(any());
    }

    @Test
    void openProvisionsPendingTenant() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT401"), any())).thenReturn(
                new Tenant("TENANT401", "Acme", TenantStatus.PENDING, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", null, null));

        assertEquals("TENANT401", service().open("tenant401"));
        verify(tenantProvisioner).provision("TENANT401");
    }

    @Test
    void disableUpdatesEnabledTenantToDisabled() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT500"), any())).thenReturn(
                new Tenant("TENANT500", "Acme", TenantStatus.ENABLED, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", null, null));

        service().disable("tenant500");

        verify(tenantDao).updateStatus(eq("TENANT500"), eq(TenantStatus.DISABLED.code()));
    }

    @Test
    void enableUpdatesDisabledTenantToEnabled() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT600"), any())).thenReturn(
                new Tenant("TENANT600", "Acme", TenantStatus.DISABLED, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", null, null));

        service().enable("tenant600");

        verify(tenantDao).updateStatus(eq("TENANT600"), eq(TenantStatus.ENABLED.code()));
    }

    @Test
    void enableRejectsExpiredTenant() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT7"), any())).thenReturn(
                new Tenant("TENANT7", "Acme", TenantStatus.DISABLED, "Owner", "13800000000",
                        LocalDate.now().minusDays(1), "remark", null, null));

        BizException ex = assertThrows(BizException.class, () -> service().enable("tenant7"));

        assertEquals("system.tenant_status_invalid", ex.getErrorCode().code());
        verify(tenantDao, never()).updateStatus(any(), anyInt());
    }

    @Test
    void getReturnsMapperValue() {
        when(tenantCache.getOrLoadTransactional(eq("TENANT8"), any())).thenReturn(
                new Tenant("TENANT8", "Acme", TenantStatus.ENABLED, "Owner", "13800000000",
                        LocalDate.now().plusDays(30), "remark", null, null));

        Tenant tenant = service().get("tenant8");

        assertEquals("TENANT8", tenant.getId());
    }
}
