package io.rosecloud.system.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantProfileData;
import io.rosecloud.system.domain.TenantProfileRepository;
import io.rosecloud.system.domain.TenantRepository;
import io.rosecloud.system.error.SystemErrorCode;
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProfileServiceImplTest {

    @Mock
    TenantProfileRepository tenantProfileRepository;
    @Mock
    TenantRepository tenantRepository;

    private TenantProfileServiceImpl service() {
        return new TenantProfileServiceImpl(tenantProfileRepository, tenantRepository);
    }

    @Test
    void createRejectsDuplicateId() {
        when(tenantProfileRepository.existsById("basic")).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> service().create(
                new TenantProfileCreateRequest("basic", "Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_EXISTS, ex.getErrorCode());
    }

    @Test
    void createRejectsBlankId() {
        BizException ex = assertThrows(BizException.class, () -> service().create(
                new TenantProfileCreateRequest(" ", "Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_ID_REQUIRED, ex.getErrorCode());
        verifyNoInteractions(tenantProfileRepository, tenantRepository);
    }

    @Test
    void createStoresProfile() {
        when(tenantProfileRepository.existsById("pro")).thenReturn(false);

        String id = service().create(new TenantProfileCreateRequest("pro", "Pro", "Production tier",
                new TenantProfileData("pro", 50, 20, 500, 120, List.of("mfa"))));

        assertEquals("pro", id);
        verify(tenantProfileRepository).insert(new TenantProfile("pro", "Pro", "Production tier",
                new TenantProfileData("pro", 50, 20, 500, 120, List.of("mfa"))));
    }

    @Test
    void updateRejectsMissingProfile() {
        when(tenantProfileRepository.findById("missing")).thenReturn(Optional.empty());

        BizException ex = assertThrows(BizException.class, () -> service().update("missing",
                new TenantProfileUpdateRequest("Basic", "Default tier", TenantProfileData.defaults())));

        assertEquals(SystemErrorCode.TENANT_PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteRejectsDefaultProfile() {
        TenantProfile profile = new TenantProfile("default", "Basic", "Default tier", true,
                (com.fasterxml.jackson.databind.JsonNode) null);
        when(tenantProfileRepository.findById("default")).thenReturn(Optional.of(profile));

        BizException ex = assertThrows(BizException.class, () -> service().delete("default"));

        assertEquals(SystemErrorCode.TENANT_PROFILE_DEFAULT_DELETE_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void deleteRejectsProfileInUse() {
        TenantProfile profile = new TenantProfile("pro", "Pro", "Production tier",
                TenantProfileData.defaults());
        when(tenantProfileRepository.findById("pro")).thenReturn(Optional.of(profile));
        when(tenantRepository.countByTenantProfileId("pro")).thenReturn(2L);

        BizException ex = assertThrows(BizException.class, () -> service().delete("pro"));

        assertEquals(SystemErrorCode.TENANT_PROFILE_IN_USE, ex.getErrorCode());
    }

    @Test
    void deleteDeletesUnusedProfile() {
        TenantProfile profile = new TenantProfile("pro", "Pro", "Production tier",
                TenantProfileData.defaults());
        when(tenantProfileRepository.findById("pro")).thenReturn(Optional.of(profile));
        when(tenantRepository.countByTenantProfileId("pro")).thenReturn(0L);

        service().delete("pro");

        verify(tenantProfileRepository).deleteById("pro");
    }

    @Test
    void makeDefaultDelegatesToRepository() {
        TenantProfile profile = new TenantProfile("pro", "Pro", "Production tier",
                TenantProfileData.defaults());
        when(tenantProfileRepository.findById("pro")).thenReturn(Optional.of(profile));

        service().makeDefault("pro");

        verify(tenantProfileRepository).makeDefault("pro");
    }

    @Test
    void getDefaultReturnsDefaultProfile() {
        TenantProfile profile = new TenantProfile("default", "Basic", "Default tier", true,
                new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(TenantProfileData.defaults()));
        when(tenantProfileRepository.findDefault()).thenReturn(Optional.of(profile));

        assertEquals(profile, service().getDefault());
    }

    @Test
    void listDelegatesToRepository() {
        TenantProfile profile = new TenantProfile("default", "Basic", "Default tier", true,
                new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(TenantProfileData.defaults()));
        when(tenantProfileRepository.findAll()).thenReturn(List.of(profile));

        assertEquals(List.of(profile), service().list());
    }
}
