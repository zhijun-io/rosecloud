package io.rosecloud.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.system.domain.TenantProfile;
import io.rosecloud.system.domain.TenantProfileData;
import io.rosecloud.system.service.TenantProfileService;
import io.rosecloud.system.service.dto.TenantProfileCreateRequest;
import io.rosecloud.system.service.dto.TenantProfileUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenantProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    TenantProfileService tenantProfileService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new TenantProfileController(tenantProfileService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createDelegatesToService() throws Exception {
        when(tenantProfileService.create(any(TenantProfileCreateRequest.class))).thenReturn("pro");

        mockMvc.perform(post("/api/system/tenant-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"pro","name":"Pro","description":"Production tier",
                                "profileData":{"packageCode":"pro","maxUsers":50,"maxRoles":20,
                                "maxNoticesPerDay":500,"maxRequestsPerMinute":120,"enabledCapabilities":["mfa"]}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("pro"));

        verify(tenantProfileService).create(any(TenantProfileCreateRequest.class));
    }

    @Test
    void updateDelegatesToService() throws Exception {
        mockMvc.perform(put("/api/system/tenant-profiles/pro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pro","description":"Production tier",
                                "profileData":{"packageCode":"pro","maxUsers":50,"maxRoles":20,
                                "maxNoticesPerDay":500,"maxRequestsPerMinute":120,"enabledCapabilities":["mfa"]}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tenantProfileService).update(any(), any(TenantProfileUpdateRequest.class));
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/system/tenant-profiles/pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tenantProfileService).delete("pro");
    }

    @Test
    void makeDefaultDelegatesToService() throws Exception {
        mockMvc.perform(put("/api/system/tenant-profiles/pro/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tenantProfileService).makeDefault("pro");
    }

    @Test
    void getReturnsProfileData() throws Exception {
        TenantProfile profile = new TenantProfile("pro", "Pro", "Production tier",
                new TenantProfileData("pro", 50, 20, 500, 120, List.of("mfa")));
        when(tenantProfileService.get("pro")).thenReturn(profile);

        mockMvc.perform(get("/api/system/tenant-profiles/pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("pro"))
                .andExpect(jsonPath("$.data.profileData.packageCode").value("pro"));
    }

    @Test
    void getDefaultReturnsProfileData() throws Exception {
        TenantProfile profile = new TenantProfile("default", "Basic", "Default tier", true,
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                        .put("packageCode", "basic")
                        .put("maxUsers", 10)
                        .put("maxRoles", 5)
                        .put("maxNoticesPerDay", 100)
                        .put("maxRequestsPerMinute", 60));
        when(tenantProfileService.getDefault()).thenReturn(profile);

        mockMvc.perform(get("/api/system/tenant-profiles/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("default"));
    }

    @Test
    void listReturnsProfiles() throws Exception {
        TenantProfile profile = new TenantProfile("default", "Basic", "Default tier",
                TenantProfileData.defaults());
        when(tenantProfileService.list()).thenReturn(List.of(profile));

        mockMvc.perform(get("/api/system/tenant-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("default"));
    }
}
