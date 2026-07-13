package io.rosecloud.auth.service;

import io.rosecloud.api.user.TenantAccessCandidate;
import io.rosecloud.api.user.UserTenantApi;
import io.rosecloud.api.user.TenantLookupApi;
import io.rosecloud.api.audit.AuditLogApi;
import io.rosecloud.auth.service.dto.TenantSelectionResponse;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSelectionServiceTest {

    @Mock
    UserTenantApi userTenantApi;
    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOperations;
    @Mock
    LoginSessionService loginSessionService;
    @Mock
    JwtTokenFactory tokenFactory;
    @Mock
    TenantLookupApi tenantLookupApi;
    @Mock
    AuditLogApi auditLogApi;

    private TenantSelectionService service() {
        SecurityProperties properties = new SecurityProperties();
        properties.setRefreshTokenExpirationSeconds(86400);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return new TenantSelectionService(userTenantApi, redisTemplate, loginSessionService, tokenFactory, properties, tenantLookupApi, auditLogApi);
    }

    @Test
    void resolveInitialTenantPrefersRememberedSelectableTenant() {
        SecurityUser user = user("TENANT1");
        when(valueOperations.get("auth:last-tenant:1")).thenReturn("TENANT2");
        when(userTenantApi.listTenantCandidates(1L)).thenReturn(ApiResponse.ok(List.of(
                candidate("TENANT1", "Tenant 1", true),
                candidate("TENANT2", "Tenant 2", true))));

        String tenantId = service().resolveInitialTenant(user);

        assertEquals("TENANT2", tenantId);
        verify(valueOperations).set(eq("auth:last-tenant:1"), eq("TENANT2"), any(Duration.class));
    }

    @Test
    void getSelectionReturnsCurrentAndSwitchableTenants() {
        SecurityUser user = user("TENANT1");
        when(valueOperations.get("auth:last-tenant:1")).thenReturn("TENANT2");
        when(userTenantApi.listTenantCandidates(1L)).thenReturn(ApiResponse.ok(List.of(
                candidate("TENANT1", "Tenant 1", true),
                candidate("TENANT3", "Tenant 3", false),
                candidate("TENANT2", "Tenant 2", true))));

        TenantSelectionResponse response = service().getSelection(user);

        assertEquals("TENANT1", response.currentTenantId());
        assertEquals("TENANT2", response.rememberedTenantId());
        assertEquals(2, response.switchableTenants().size());
    }

    @Test
    void switchTenantReissuesTokensAndReplacesCurrentSession() {
        SecurityUser user = user("TENANT1");
        when(userTenantApi.listTenantCandidates(1L)).thenReturn(ApiResponse.ok(List.of(
                candidate("TENANT2", "Tenant 2", true))));
        when(tokenFactory.createTokenPair(user, "TENANT2")).thenReturn(new JwtPair("access-2", "refresh-2"));

        JwtPair pair = service().switchTenant(user, "old-token", "TENANT2", "127.0.0.1", "JUnit");

        assertEquals("access-2", pair.accessToken());
        verify(loginSessionService).save(any(io.rosecloud.common.security.model.LoginSession.class));
        verify(loginSessionService).revoke("old-token");
        verify(valueOperations).set(eq("auth:last-tenant:1"), eq("TENANT2"), any(Duration.class));
    }

    @Test
    void resolveInitialTenantRejectsWhenNoSelectableTenantExists() {
        SecurityUser user = user("TENANT1");
        when(valueOperations.get("auth:last-tenant:1")).thenReturn(null);
        when(userTenantApi.listTenantCandidates(1L)).thenReturn(ApiResponse.ok(List.of(
                candidate("TENANT2", "Tenant 2", false))));

        assertThrows(io.rosecloud.common.core.error.BizException.class,
                () -> service().resolveInitialTenant(user));
    }

    @Test
    void impersonateCreatesTokenForEnabledTenant() {
        SecurityUser admin = systemAdmin();
        when(tenantLookupApi.findTenantStatus("TENANT-B")).thenReturn(ApiResponse.ok(
                new io.rosecloud.api.user.TenantStatusView("TENANT-B", "ENABLED")));
        when(tokenFactory.createTokenPair(any(SecurityUser.class), eq("TENANT-B")))
                .thenReturn(new JwtPair("imp-access", "imp-refresh"));

        JwtPair pair = service().impersonate(admin, "admin-token", "tenant-b", "10.0.0.1", "AdminUI");

        assertEquals("imp-access", pair.accessToken());
        verify(loginSessionService).save(any(io.rosecloud.common.security.model.LoginSession.class));
        verify(loginSessionService).revoke("admin-token");
    }

    @Test
    void impersonateCreatesTokenForExpiredTenant() {
        SecurityUser admin = systemAdmin();
        when(tenantLookupApi.findTenantStatus("TENANT-C")).thenReturn(ApiResponse.ok(
                new io.rosecloud.api.user.TenantStatusView("TENANT-C", "EXPIRED")));
        when(tokenFactory.createTokenPair(any(SecurityUser.class), eq("TENANT-C")))
                .thenReturn(new JwtPair("imp-access-c", "imp-refresh-c"));

        JwtPair pair = service().impersonate(admin, "admin-token", "tenant-c", "10.0.0.1", "AdminUI");

        assertEquals("imp-access-c", pair.accessToken());
        verify(loginSessionService).save(any(io.rosecloud.common.security.model.LoginSession.class));
    }

    @Test
    void impersonateRejectsPendingTenant() {
        SecurityUser admin = systemAdmin();
        when(tenantLookupApi.findTenantStatus("TENANT-D")).thenReturn(ApiResponse.ok(
                new io.rosecloud.api.user.TenantStatusView("TENANT-D", "PENDING")));

        assertThrows(io.rosecloud.common.core.error.BizException.class,
                () -> service().impersonate(admin, "admin-token", "tenant-d", "10.0.0.1", "AdminUI"));
    }

    @Test
    void impersonateRejectsDisabledTenant() {
        SecurityUser admin = systemAdmin();
        when(tenantLookupApi.findTenantStatus("TENANT-E")).thenReturn(ApiResponse.ok(
                new io.rosecloud.api.user.TenantStatusView("TENANT-E", "DISABLED")));

        assertThrows(io.rosecloud.common.core.error.BizException.class,
                () -> service().impersonate(admin, "admin-token", "tenant-e", "10.0.0.1", "AdminUI"));
    }

    @Test
    void impersonateRejectsNonPlatformAdmin() {
        SecurityUser regularUser = user("ACME");

        assertThrows(io.rosecloud.common.core.error.BizException.class,
                () -> service().impersonate(regularUser, "user-token", "TENANT-F", "10.0.0.1", "AdminUI"));
    }

    private static SecurityUser user(String tenantId) {
        return new SecurityUser(1L, "alice@example.com", "Alice", "hash", true, tenantId,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "alice@example.com"), List.of());
    }

    private static TenantAccessCandidate candidate(String tenantId, String tenantName, boolean selectable) {
        return new TenantAccessCandidate(tenantId, tenantName, "ENABLED", selectable);
    }

    private static SecurityUser systemAdmin() {
        return new SecurityUser(99L, "root@platform.com", "PlatformAdmin", "hash", true, "ROOT",
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "root@platform.com"), List.of());
    }
}
