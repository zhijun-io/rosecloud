package io.rosecloud.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.PageQuery;
import io.rosecloud.common.core.model.PagedData;
import io.rosecloud.starter.web.PageQueryArgumentResolver;
import io.rosecloud.system.domain.AuditLog;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantProfileData;
import io.rosecloud.system.domain.TenantStatus;
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

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new TenantController(tenantService))
                .setCustomArgumentResolvers(new PageQueryArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createDelegatesToTenantService() throws Exception {
        when(tenantService.create(any(TenantCreateRequest.class))).thenReturn("TENANT1");

        mockMvc.perform(post("/api/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"tenantId":"tenant1","name":"Acme","contactUser":"Owner","contactPhone":"13800000000",
                                "expireTime":"%s","remark":"remark","tenantProfileId":"profile-1",
                                "adminUsername":"admin"}
                                """.formatted(LocalDate.now().plusDays(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("TENANT1"));

        verify(tenantService).create(any(TenantCreateRequest.class));
    }

    @Test
    void updateDelegatesToTenantService() throws Exception {
        mockMvc.perform(put("/api/tenants/TENANT1")
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
        mockMvc.perform(delete("/api/tenants/TENANT1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tenantService).delete("TENANT1");
    }

    @Test
    void getReturnsTenantData() throws Exception {
        Tenant tenant = new Tenant("TENANT1", "Acme", TenantStatus.ENABLED, "Owner", "13800000000",
                LocalDate.now().plusDays(30), "remark", "profile-1",
                new ObjectMapper().valueToTree(new TenantProfileData("pro", 10, 5, 100, 60, 60, 0L, 80, 90, List.of())));
        when(tenantService.get("TENANT1")).thenReturn(tenant);

        mockMvc.perform(get("/api/tenants/TENANT1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("TENANT1"))
                .andExpect(jsonPath("$.data.tenantProfileId").value("profile-1"));
    }

    @Test
    void pageReturnsTenantPage() throws Exception {
        when(tenantService.page(any(PageQuery.class))).thenReturn(PagedData.empty());

        mockMvc.perform(get("/api/tenants?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.data").isArray());
    }

    @Test
    void openDelegatesToTenantService() throws Exception {
        when(tenantService.open("TENANT1")).thenReturn("TENANT1");

        mockMvc.perform(post("/api/tenants/TENANT1/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("TENANT1"));

        verify(tenantService).open("TENANT1");
    }
}
