package io.rosecloud.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.persistence.UserEntity;
import io.rosecloud.system.persistence.UserMapper;
import io.rosecloud.system.persistence.UserTenantEntity;
import io.rosecloud.system.persistence.UserTenantMapper;
import io.rosecloud.system.service.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTenantControllerTest {

    @Mock
    UserMapper userMapper;
    @Mock
    UserTenantMapper userTenantMapper;
    @Mock
    TenantService tenantService;
    @Mock
    ObjectMapper objectMapper;

    @Test
    void listTenantCandidatesMarksSelectableTenants() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setTenantId("TENANT1");
        user.setEmail("alice@example.com");
        user.setNickname("Alice");
        user.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        UserTenantEntity link = new UserTenantEntity();
        link.setUserId(1L);
        link.setTenantId("TENANT2");
        when(userTenantMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(link));

        when(tenantService.get("TENANT1")).thenReturn(tenant("TENANT1", "Tenant 1", TenantStatus.ENABLED));
        when(tenantService.get("TENANT2")).thenReturn(tenant("TENANT2", "Tenant 2", TenantStatus.DISABLED));

        UserTenantController controller =
                new UserTenantController(userMapper, userTenantMapper, tenantService, objectMapper);

        var response = controller.listTenantCandidates(1L).data();

        assertEquals(2, response.size());
        assertEquals("TENANT1", response.get(0).tenantId());
        assertEquals(true, response.get(0).selectable());
        assertEquals("TENANT2", response.get(1).tenantId());
        assertEquals(false, response.get(1).selectable());
    }

    private static Tenant tenant(String id, String name, TenantStatus status) {
        return new Tenant(id, name, status, "Owner", "13800000000", LocalDate.now().plusDays(30), null, null, null);
    }
}
