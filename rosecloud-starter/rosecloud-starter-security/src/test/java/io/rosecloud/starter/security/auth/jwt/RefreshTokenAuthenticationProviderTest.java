package io.rosecloud.starter.security.auth.jwt;

import io.rosecloud.common.security.user.TenantLookupApi;
import io.rosecloud.common.security.user.TenantStatusView;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.starter.security.session.LoginSessionApi;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import io.rosecloud.starter.security.auth.RefreshAuthenticationToken;
import io.rosecloud.starter.security.auth.BruteForceProtection;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshTokenAuthenticationProviderTest {

    @Test
    void rejectsDisabledTenantOnRefresh() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = user("TENANT1");
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        LoginSessionApi loginSessionApi = mock(LoginSessionApi.class);
        when(loginSessionApi.isRevoked(pair.refreshToken())).thenReturn(false);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
        when(tenantLookupApi.findTenantStatus("TENANT1"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", "DISABLED")));

        RefreshTokenAuthenticationProvider provider = new RefreshTokenAuthenticationProvider(
                tokenFactory, loginSessionApi, userDetailsService, tenantLookupApi, newBruteForce());

        assertThrows(BizException.class,
                () -> provider.authenticate(new RefreshAuthenticationToken(new RawAccessJwtToken(pair.refreshToken()))));
    }

    @Test
    void keepsEnabledTenantOnRefresh() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = user("TENANT1");
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        LoginSessionApi loginSessionApi = mock(LoginSessionApi.class);
        when(loginSessionApi.isRevoked(pair.refreshToken())).thenReturn(false);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
        when(tenantLookupApi.findTenantStatus("TENANT1"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", "ENABLED")));

        RefreshTokenAuthenticationProvider provider = new RefreshTokenAuthenticationProvider(
                tokenFactory, loginSessionApi, userDetailsService, tenantLookupApi, newBruteForce());

        RefreshAuthenticationToken auth = (RefreshAuthenticationToken) provider.authenticate(
                new RefreshAuthenticationToken(new RawAccessJwtToken(pair.refreshToken())));

        assertEquals("TENANT1", ((SecurityUser) auth.getPrincipal()).getTenantId());
    }

    @Test
    void rejectsPendingAndDisabledTenantStatusesOnRefresh() {
        for (String status : new String[] { "PENDING", "DISABLED" }) {
            JwtTokenFactory tokenFactory = newFactory();
            SecurityUser user = user("TENANT1");
            JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
            LoginSessionApi loginSessionApi = mock(LoginSessionApi.class);
            when(loginSessionApi.isRevoked(pair.refreshToken())).thenReturn(false);
            UserDetailsService userDetailsService = mock(UserDetailsService.class);
            when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
            TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
            when(tenantLookupApi.findTenantStatus("TENANT1"))
                    .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", status)));

            RefreshTokenAuthenticationProvider provider = new RefreshTokenAuthenticationProvider(
                    tokenFactory, loginSessionApi, userDetailsService, tenantLookupApi, newBruteForce());

            assertThrows(BizException.class,
                    () -> provider.authenticate(new RefreshAuthenticationToken(new RawAccessJwtToken(pair.refreshToken()))));
        }
    }

    @Test
    void allowsExpiredTenantOnRefresh() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = user("TENANT1");
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        LoginSessionApi loginSessionApi = mock(LoginSessionApi.class);
        when(loginSessionApi.isRevoked(pair.refreshToken())).thenReturn(false);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);
        TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
        when(tenantLookupApi.findTenantStatus("TENANT1"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", "EXPIRED")));

        RefreshTokenAuthenticationProvider provider = new RefreshTokenAuthenticationProvider(
                tokenFactory, loginSessionApi, userDetailsService, tenantLookupApi, newBruteForce());

        RefreshAuthenticationToken auth = (RefreshAuthenticationToken) provider.authenticate(
                new RefreshAuthenticationToken(new RawAccessJwtToken(pair.refreshToken())));

        assertEquals("TENANT1", ((SecurityUser) auth.getPrincipal()).getTenantId());
    }

    @Test
    void toleratesMissingTenantLookupApiOnRefresh() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = user("TENANT1");
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        LoginSessionApi loginSessionApi = mock(LoginSessionApi.class);
        when(loginSessionApi.isRevoked(pair.refreshToken())).thenReturn(false);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(user);

        RefreshTokenAuthenticationProvider provider = new RefreshTokenAuthenticationProvider(
                tokenFactory, loginSessionApi, userDetailsService, null, newBruteForce());

        RefreshAuthenticationToken auth = (RefreshAuthenticationToken) provider.authenticate(
                new RefreshAuthenticationToken(new RawAccessJwtToken(pair.refreshToken())));

        assertEquals("TENANT1", ((SecurityUser) auth.getPrincipal()).getTenantId());
    }

    private static JwtTokenFactory newFactory() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setIssuer("rosecloud");
        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[64]));
        return new JwtTokenFactory(properties);
    }

    private static BruteForceProtection newBruteForce() {
        // No-op protection for unit tests: no Redis means throttling is disabled.
        return new BruteForceProtection(new SecurityProperties.BruteForce(), null);
    }

    private static SecurityUser user(String tenantId) {
        return new SecurityUser(1L, "alice@example.com", "Alice", "hash", true, tenantId,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "alice@example.com"), List.of());
    }
}
