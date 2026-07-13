package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.event.EntityChangedEvent;
import io.rosecloud.starter.data.cache.EntityCache;
import io.rosecloud.starter.data.event.EntityEventPublisher;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.error.SystemErrorCode;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.rosecloud.system.persistence.RoleEntity;
import io.rosecloud.system.persistence.TenantEntity;
import io.rosecloud.system.persistence.TenantMapper;
import io.rosecloud.system.persistence.TenantProfileEntity;
import io.rosecloud.system.persistence.TenantProfileMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, TenantEntity.class);
        TableInfoHelper.initTableInfo(assistant, TenantProfileEntity.class);
        TableInfoHelper.initTableInfo(assistant, RoleEntity.class);
    }

    @Mock
    TenantMapper tenantMapper;
    @Mock
    TenantProfileMapper tenantProfileMapper;
    @Mock
    TenantProvisioner tenantProvisioner;
    @Mock
    EntityCache<String, Tenant> tenantCache;
    @Mock
    EntityEventPublisher eventPublisher;

    private TenantServiceImpl service() {
        return new TenantServiceImpl(tenantMapper, tenantProfileMapper, tenantProvisioner, tenantCache, eventPublisher);
    }

    @Test
    void createPersistsTenantAndTriggersProvisioning() {
        TenantCreateRequest request = new TenantCreateRequest("tenant1", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", null, "admin");
        when(tenantProfileMapper.selectOne(any())).thenReturn(defaultProfile("profile-default"));
        when(tenantMapper.selectById("TENANT1")).thenReturn(null, entity("TENANT1", TenantStatus.PENDING, "profile-default"));

        String id = service().create(request);

        assertEquals("TENANT1", id);
        ArgumentCaptor<TenantEntity> captor = ArgumentCaptor.forClass(TenantEntity.class);
        verify(tenantMapper).insert(captor.capture());
        assertEquals("TENANT1", captor.getValue().getId());
        assertEquals(TenantStatus.PENDING.code(), captor.getValue().getStatus());
        assertEquals("profile-default", captor.getValue().getTenantProfileId());
        verify(tenantProvisioner).provision("TENANT1");
    }

    @Test
    void createUsesProvidedTenantProfile() {
        TenantCreateRequest request = new TenantCreateRequest("tenant2", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", "profile-custom", "admin");
        when(tenantProfileMapper.selectById("profile-custom")).thenReturn(customProfile("profile-custom"));
        when(tenantMapper.selectById("TENANT2")).thenReturn(null, entity("TENANT2", TenantStatus.PENDING, "profile-custom"));

        assertEquals("TENANT2", service().create(request));
        verify(tenantProvisioner).provision("TENANT2");
    }

    @Test
    void createRejectsReservedSystemTenantId() {
        TenantCreateRequest request = new TenantCreateRequest("root", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", null, "admin");

        BizException ex = assertThrows(BizException.class, () -> service().create(request));

        assertEquals(SystemErrorCode.TENANT_ID_RESERVED, ex.getErrorCode());
        verify(tenantMapper, never()).insert(any(TenantEntity.class));
    }

    @Test
    void createRejectsDuplicateTenantId() {
        when(tenantMapper.selectById("TENANT9")).thenReturn(entity("TENANT9", TenantStatus.ENABLED, null));
        TenantCreateRequest request = new TenantCreateRequest("tenant9", "Acme", "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", null, "admin");

        BizException ex = assertThrows(BizException.class, () -> service().create(request));

        assertEquals(SystemErrorCode.TENANT_CODE_EXISTS, ex.getErrorCode());
        verify(tenantMapper, never()).insert(any(TenantEntity.class));
    }

    @Test
    void updateReusesExistingProfileWhenNotSpecified() {
        when(tenantMapper.selectById("TENANT200")).thenReturn(entity("TENANT200", TenantStatus.ENABLED, "profile-default"));
        when(tenantProfileMapper.selectById("profile-default")).thenReturn(defaultProfile("profile-default"));

        service().update("tenant200", new TenantUpdateRequest("New", "Owner-2", "13900000000",
                LocalDate.now().plusDays(60), "new remark", null));

        verify(tenantMapper).update(any(), any());
    }

    @Test
    void deleteDelegatesToMapper() {
        when(tenantMapper.selectById("TENANT300")).thenReturn(entity("TENANT300", TenantStatus.ENABLED, null));

        service().delete("tenant300");

        verify(tenantMapper).deleteById("TENANT300");
    }

    @Test
    void openRejectsEnabledTenant() {
        when(tenantMapper.selectById("TENANT400")).thenReturn(entity("TENANT400", TenantStatus.ENABLED, null));

        BizException ex = assertThrows(BizException.class, () -> service().open("tenant400"));

        assertEquals(SystemErrorCode.TENANT_STATUS_INVALID, ex.getErrorCode());
        verify(tenantProvisioner, never()).provision(any());
    }

    @Test
    void openProvisionsPendingTenant() {
        when(tenantMapper.selectById("TENANT401")).thenReturn(entity("TENANT401", TenantStatus.PENDING, null));

        assertEquals("TENANT401", service().open("tenant401"));
        verify(tenantProvisioner).provision("TENANT401");
    }

    @Test
    void disableUpdatesEnabledTenantToDisabled() {
        when(tenantMapper.selectById("TENANT500")).thenReturn(entity("TENANT500", TenantStatus.ENABLED, null));

        service().disable("tenant500");

        verify(tenantMapper).update(any(), any());
    }

    @Test
    void enableUpdatesDisabledTenantToEnabled() {
        when(tenantMapper.selectById("TENANT600")).thenReturn(entity("TENANT600", TenantStatus.DISABLED, null));

        service().enable("tenant600");

        verify(tenantMapper).update(any(), any());
    }

    @Test
    void enableRejectsExpiredTenant() {
        TenantEntity expired = entity("TENANT7", TenantStatus.DISABLED, null);
        expired.setExpireTime(LocalDate.now().minusDays(1));
        when(tenantMapper.selectById("TENANT7")).thenReturn(expired);

        BizException ex = assertThrows(BizException.class, () -> service().enable("tenant7"));

        assertEquals("system.tenant_status_invalid", ex.getErrorCode().code());
        verify(tenantMapper, never()).update(any(), any());
    }

    @Test
    void getReturnsMapperValue() {
        when(tenantMapper.selectById("TENANT8")).thenReturn(entity("TENANT8", TenantStatus.ENABLED, null));

        Tenant tenant = service().get("tenant8");

        assertEquals("TENANT8", tenant.getId());
    }

    private static TenantEntity entity(String id, TenantStatus status, String profileId) {
        TenantEntity e = new TenantEntity();
        e.setId(id);
        e.setName("Acme");
        e.setStatus(status.code());
        e.setContactUser("Owner");
        e.setContactPhone("13800000000");
        e.setExpireTime(LocalDate.now().plusDays(30));
        e.setRemark("remark");
        e.setTenantProfileId(profileId);
        return e;
    }

    private static TenantProfileEntity defaultProfile(String id) {
        TenantProfileEntity e = new TenantProfileEntity();
        e.setId(id);
        e.setIsDefault(true);
        return e;
    }

    private static TenantProfileEntity customProfile(String id) {
        TenantProfileEntity e = new TenantProfileEntity();
        e.setId(id);
        e.setIsDefault(false);
        return e;
    }
}
