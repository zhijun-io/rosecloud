package io.rosecloud.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantProfileData;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.service.AuditLogService;
import io.rosecloud.system.service.TenantService;
import io.rosecloud.system.service.dto.TenantCreateRequest;
import io.rosecloud.system.service.dto.TenantUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
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
class TenantControllerTest {

    private MockMvc mockMvc;

    @Mock
    TenantService tenantService;
    @Mock
    AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new TenantController(tenantService, auditLogService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createDelegatesToTenantService() throws Exception {
        when(tenantService.create(any(TenantCreateRequest.class))).thenReturn("tenant-1");

        mockMvc.perform(post("/api/system/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"name":"Acme","contactUser":"Owner","contactPhone":"13800000000",
                                "expireTime":"%s","remark":"remark","tenantProfileId":"profile-1",
                                "adminUsername":"admin"}
                                """.formatted(LocalDate.now().plusDays(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("tenant-1"));

        verify(tenantService).create(any(TenantCreateRequest.class));
    }

    @Test
    void updateDelegatesToTenantService() throws Exception {
        mockMvc.perform(put("/api/system/tenants/tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme","contactUser":"Owner","contactPhone":"13800000000",
                                "expireTime":"%s","remark":"remark","tenantProfileId":"profile-1"}
                                """.formatted(LocalDate.now().plusDays(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tenantService).update(any(), any(TenantUpdateRequest.class));
    }

    @Test
    void deleteDelegatesToTenantService() throws Exception {
        mockMvc.perform(delete("/api/system/tenants/tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tenantService).delete("tenant-1");
    }

    @Test
    void getReturnsTenantData() throws Exception {
        Tenant tenant = new Tenant("tenant-1", "Acme", TenantStatus.ENABLED, "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", "profile-1",
                new ObjectMapper().valueToTree(new TenantProfileData("pro", 10, 5, 100, 60, List.of())));
        when(tenantService.get("tenant-1")).thenReturn(tenant);

        mockMvc.perform(get("/api/system/tenants/tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("tenant-1"))
                .andExpect(jsonPath("$.data.tenantProfileId").value("profile-1"));
    }

    @Test
    void pageReturnsTenantPage() throws Exception {
        when(tenantService.page(1, 10, null)).thenReturn(PageResult.of(List.of(), 0, 1, 10));

        mockMvc.perform(get("/api/system/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void openDelegatesToTenantService() throws Exception {
        when(tenantService.open("tenant-1")).thenReturn("tenant-1");

        mockMvc.perform(post("/api/system/tenants/tenant-1/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("tenant-1"));

        verify(tenantService).open("tenant-1");
    }
}
