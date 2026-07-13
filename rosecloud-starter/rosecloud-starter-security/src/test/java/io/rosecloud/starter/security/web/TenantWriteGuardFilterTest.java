package io.rosecloud.starter.security.web;

import io.rosecloud.common.security.user.TenantLookupApi;
import io.rosecloud.common.security.user.TenantStatusView;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantWriteGuardFilterTest {

    @Mock
    TenantLookupApi tenantLookupApi;
    @Mock
    ObjectProvider<TenantLookupApi> provider;
    @Mock
    FilterChain filterChain;

    private TenantWriteGuardFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantWriteGuardFilter(provider, true);
    }

    @Test
    void safeMethodsPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void impersonationSessionBlocksWrite() throws Exception {
        SecurityUser impersonatedUser = new SecurityUser(1L, "admin@test.com", "Admin",
                "hash", true, "TENANT1",
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "admin@test.com"), List.of())
                .withImpersonation(true);
        SecurityContextHolder.getContext().setAuthentication(new AuthStub(impersonatedUser));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tenants/TENANT1/disable");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
        SecurityContextHolder.clearContext();
    }

    @Test
    void expiredTenantBlocksWrite() throws Exception {
        SecurityUser user = new SecurityUser(1L, "admin@test.com", "Admin",
                "hash", true, "TENANT2",
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "admin@test.com"), List.of());
        SecurityContextHolder.getContext().setAuthentication(new AuthStub(user));
        when(provider.getIfAvailable()).thenReturn(tenantLookupApi);
        when(tenantLookupApi.findTenantStatus("TENANT2"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT2", "EXPIRED")));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
        SecurityContextHolder.clearContext();
    }

    @Test
    void enabledTenantAllowsWrite() throws Exception {
        SecurityUser user = new SecurityUser(1L, "admin@test.com", "Admin",
                "hash", true, "TENANT3",
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "admin@test.com"), List.of());
        SecurityContextHolder.getContext().setAuthentication(new AuthStub(user));
        when(provider.getIfAvailable()).thenReturn(tenantLookupApi);
        when(tenantLookupApi.findTenantStatus("TENANT3"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT3", "ENABLED")));

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/tenants/TENANT3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }

    private static class AuthStub implements Authentication {
        private final SecurityUser principal;
        private boolean authenticated = true;

        AuthStub(SecurityUser principal) {
            this.principal = principal;
        }

        @Override
        public String getName() { return principal.getUsername(); }

        @Override
        public Object getCredentials() { return null; }

        @Override
        public Object getDetails() { return null; }

        @Override
        public Object getPrincipal() { return principal; }

        @Override
        public boolean isAuthenticated() { return authenticated; }

        @Override
        public void setAuthenticated(boolean isAuthenticated) {
            this.authenticated = isAuthenticated;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return principal.getAuthorities();
        }
    }
}
