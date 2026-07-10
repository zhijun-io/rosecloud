package io.rosecloud.system.controller;

import io.rosecloud.system.domain.Tenant;
import io.rosecloud.system.domain.TenantStatus;
import io.rosecloud.system.domain.User;
import io.rosecloud.system.domain.UserRepository;
import io.rosecloud.system.service.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTenantControllerTest {

    @Mock
    UserRepository userRepository;
    @Mock
    TenantService tenantService;

    @Test
    void listTenantCandidatesMarksSelectableTenants() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user("TENANT1")));
        when(userRepository.findTenantIdsByUserId(1L)).thenReturn(List.of("TENANT2"));
        when(tenantService.get("TENANT1")).thenReturn(tenant("TENANT1", "Tenant 1", TenantStatus.ENABLED));
        when(tenantService.get("TENANT2")).thenReturn(tenant("TENANT2", "Tenant 2", TenantStatus.DISABLED));

        UserTenantController controller = new UserTenantController(userRepository, tenantService);

        var response = controller.listTenantCandidates(1L).data();

        assertEquals(2, response.size());
        assertEquals("TENANT1", response.get(0).tenantId());
        assertEquals(true, response.get(0).selectable());
        assertEquals("TENANT2", response.get(1).tenantId());
        assertEquals(false, response.get(1).selectable());
    }

    private static User user(String tenantId) {
        return new User(1L, "alice@example.com", "Alice", 1, tenantId, null);
    }

    private static Tenant tenant(String id, String name, TenantStatus status) {
        return new Tenant(id, name, status, "Owner", "13800000000", LocalDate.now().plusDays(30), null, null, null);
    }
}
