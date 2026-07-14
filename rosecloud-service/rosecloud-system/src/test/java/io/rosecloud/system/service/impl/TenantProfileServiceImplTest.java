package io.rosecloud.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantProfileData;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.persistence.TenantDao;
import io.rosecloud.system.persistence.TenantProfileDao;
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProfileServiceImplTest {

    @Mock
    TenantProfileDao tenantProfileDao;
    @Mock
    TenantDao tenantDao;

    @InjectMocks
    TenantProfileServiceImpl service;

    @Captor
    ArgumentCaptor<TenantProfile> profileCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createRejectsDuplicateId() {
        when(tenantProfileDao.findById("basic")).thenReturn(Optional.of(profile("basic", false)));

        BizException ex = assertThrows(BizException.class, () -> service.create(
                new TenantProfileCreateRequest("basic", "Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_EXISTS, ex.getErrorCode());
    }

    @Test
    void createRejectsBlankId() {
        BizException ex = assertThrows(BizException.class, () -> service.create(
                new TenantProfileCreateRequest(" ", "Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_ID_REQUIRED, ex.getErrorCode());
        verifyNoInteractions(tenantProfileDao, tenantDao);
    }

    @Test
    void createStoresProfile() {
        when(tenantProfileDao.findById("pro")).thenReturn(Optional.empty());

        String id = service.create(new TenantProfileCreateRequest("pro", "Pro", "Production tier",
                new TenantProfileData("pro", 50, 20, 500, 120, 60, 0L, 80, 90, List.of("mfa"))));

        assertEquals("pro", id);
        verify(tenantProfileDao).save(profileCaptor.capture());
        assertEquals("pro", profileCaptor.getValue().getId());
        assertEquals("Pro", profileCaptor.getValue().getName());
    }

    @Test
    void updateRejectsMissingProfile() {
        when(tenantProfileDao.findById("missing")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service.update("missing",
                new TenantProfileUpdateRequest("Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteRejectsDefaultProfile() {
        when(tenantProfileDao.findById("default")).thenReturn(Optional.of(profile("default", true)));

        BizException ex = assertThrows(BizException.class, () -> service.delete("default"));

        assertEquals(SystemErrorCode.TENANT_PROFILE_DEFAULT_DELETE_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void deleteRejectsProfileInUse() {
        when(tenantProfileDao.findById("pro")).thenReturn(Optional.of(profile("pro", false)));
        when(tenantDao.countByProfileId("pro")).thenReturn(2L);

        BizException ex = assertThrows(BizException.class, () -> service.delete("pro"));

        assertEquals(SystemErrorCode.TENANT_PROFILE_IN_USE, ex.getErrorCode());
    }

    @Test
    void deleteDeletesUnusedProfile() {
        when(tenantProfileDao.findById("pro")).thenReturn(Optional.of(profile("pro", false)));
        when(tenantDao.countByProfileId("pro")).thenReturn(0L);

        service.delete("pro");

        verify(tenantProfileDao).removeById("pro");
    }

    @Test
    void makeDefaultDelegatesToDao() {
        when(tenantProfileDao.findById("pro")).thenReturn(Optional.of(profile("pro", false)));

        service.makeDefault("pro");

        verify(tenantProfileDao).makeDefault("pro");
    }

    @Test
    void getDefaultReturnsDefaultProfile() {
        TenantProfile expected = new TenantProfile("default", "Basic", "Default tier", true,
                objectMapper.valueToTree(TenantProfileData.defaults()));
        when(tenantProfileDao.findDefault()).thenReturn(Optional.of(expected));

        assertEquals(expected, service.getDefault());
    }

    @Test
    void listDelegatesToDao() {
        TenantProfile expected = new TenantProfile("default", "Basic", "Default tier", true,
                objectMapper.valueToTree(TenantProfileData.defaults()));
        when(tenantProfileDao.findAllOrdered()).thenReturn(List.of(expected));

        assertEquals(List.of(expected), service.list());
    }

    private static TenantProfile profile(String id, boolean isDefault) {
        return new TenantProfile(id, "Pro", "tier", isDefault,
                new ObjectMapper().valueToTree(TenantProfileData.defaults()));
    }
}
