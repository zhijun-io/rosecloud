package io.rosecloud.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantProfileData;
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
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProfileServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, TenantEntity.class);
        TableInfoHelper.initTableInfo(assistant, TenantProfileEntity.class);
        TableInfoHelper.initTableInfo(assistant, RoleEntity.class);
    }

    @Mock
    TenantProfileMapper tenantProfileMapper;
    @Mock
    TenantMapper tenantMapper;

    private TenantProfileServiceImpl service() {
        return new TenantProfileServiceImpl(tenantProfileMapper, tenantMapper);
    }

    @Test
    void createRejectsDuplicateId() {
        when(tenantProfileMapper.selectById("basic")).thenReturn(profileEntity("basic", false));

        BizException ex = assertThrows(BizException.class, () -> service().create(
                new TenantProfileCreateRequest("basic", "Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_EXISTS, ex.getErrorCode());
    }

    @Test
    void createRejectsBlankId() {
        BizException ex = assertThrows(BizException.class, () -> service().create(
                new TenantProfileCreateRequest(" ", "Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_ID_REQUIRED, ex.getErrorCode());
        verifyNoInteractions(tenantProfileMapper, tenantMapper);
    }

    @Test
    void createStoresProfile() {
        when(tenantProfileMapper.selectById("pro")).thenReturn(null);

        String id = service().create(new TenantProfileCreateRequest("pro", "Pro", "Production tier",
                new TenantProfileData("pro", 50, 20, 500, 120, 60, 0L, 80, 90, List.of("mfa"))));

        assertEquals("pro", id);
        verify(tenantProfileMapper).insert(any(TenantProfileEntity.class));
    }

    @Test
    void updateRejectsMissingProfile() {
        when(tenantProfileMapper.selectById("missing")).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service().update("missing",
                new TenantProfileUpdateRequest("Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteRejectsDefaultProfile() {
        when(tenantProfileMapper.selectById("default")).thenReturn(profileEntity("default", true));

        BizException ex = assertThrows(BizException.class, () -> service().delete("default"));

        assertEquals(SystemErrorCode.TENANT_PROFILE_DEFAULT_DELETE_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void deleteRejectsProfileInUse() {
        when(tenantProfileMapper.selectById("pro")).thenReturn(profileEntity("pro", false));
        when(tenantMapper.selectCount(any())).thenReturn(2L);

        BizException ex = assertThrows(BizException.class, () -> service().delete("pro"));

        assertEquals(SystemErrorCode.TENANT_PROFILE_IN_USE, ex.getErrorCode());
    }

    @Test
    void deleteDeletesUnusedProfile() {
        when(tenantProfileMapper.selectById("pro")).thenReturn(profileEntity("pro", false));
        when(tenantMapper.selectCount(any())).thenReturn(0L);

        service().delete("pro");

        verify(tenantProfileMapper).deleteById("pro");
    }

    @Test
    void makeDefaultDelegatesToMapper() {
        when(tenantProfileMapper.selectById("pro")).thenReturn(profileEntity("pro", false));

        service().makeDefault("pro");

        verify(tenantProfileMapper).update(any(), any());
    }

    @Test
    void getDefaultReturnsDefaultProfile() {
        TenantProfileEntity e = new TenantProfileEntity();
        e.setId("default");
        e.setName("Basic");
        e.setDescription("Default tier");
        e.setIsDefault(true);
        when(tenantProfileMapper.selectOne(any())).thenReturn(e);

        TenantProfile profile = new TenantProfile("default", "Basic", "Default tier", true,
                new ObjectMapper().valueToTree(TenantProfileData.defaults()));

        assertEquals(profile, service().getDefault());
    }

    @Test
    void listDelegatesToMapper() {
        TenantProfileEntity e = new TenantProfileEntity();
        e.setId("default");
        e.setName("Basic");
        e.setDescription("Default tier");
        e.setIsDefault(true);
        when(tenantProfileMapper.selectList(any())).thenReturn(List.of(e));

        TenantProfile profile = new TenantProfile("default", "Basic", "Default tier", true,
                new ObjectMapper().valueToTree(TenantProfileData.defaults()));

        assertEquals(List.of(profile), service().list());
    }

    private static TenantProfileEntity profileEntity(String id, boolean isDefault) {
        TenantProfileEntity e = new TenantProfileEntity();
        e.setId(id);
        e.setName("Pro");
        e.setDescription("tier");
        e.setIsDefault(isDefault);
        return e;
    }
}
