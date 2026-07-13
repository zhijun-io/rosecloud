package io.rosecloud.starter.security.auth.jwt;

import io.rosecloud.api.user.TenantLookupApi;
import io.rosecloud.api.user.TenantStatusView;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.common.security.token.RawAccessJwtToken;
import io.rosecloud.starter.security.auth.JwtAuthenticationToken;
import io.rosecloud.starter.security.config.SecurityProperties;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationProviderTest {

    @Test
    void rejectsDisabledTenantAtAuthenticationTime() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = user("TENANT1");
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        SessionStore sessionStore = mock(SessionStore.class);
        when(sessionStore.isRevoked(pair.accessToken())).thenReturn(false);
        TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
        when(tenantLookupApi.findTenantStatus("TENANT1"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", "DISABLED")));

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(
                tokenFactory, sessionStore, tenantLookupApi);

        assertThrows(BizException.class,
                () -> provider.authenticate(new JwtAuthenticationToken(new RawAccessJwtToken(pair.accessToken()))));
    }

    @Test
    void allowsEnabledTenantAtAuthenticationTime() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = user("TENANT1");
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        SessionStore sessionStore = mock(SessionStore.class);
        when(sessionStore.isRevoked(pair.accessToken())).thenReturn(false);
        TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
        when(tenantLookupApi.findTenantStatus("TENANT1"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", "ENABLED")));

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(
                tokenFactory, sessionStore, tenantLookupApi);

        JwtAuthenticationToken auth = (JwtAuthenticationToken) provider.authenticate(
                new JwtAuthenticationToken(new RawAccessJwtToken(pair.accessToken())));

        assertEquals("TENANT1", ((SecurityUser) auth.getPrincipal()).getTenantId());
    }

    @Test
    void rejectsPendingAndDisabledTenantStatusesAtAuthenticationTime() {
        for (String status : new String[] { "PENDING", "DISABLED" }) {
            JwtTokenFactory tokenFactory = newFactory();
            SecurityUser user = user("TENANT1");
            JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
            SessionStore sessionStore = mock(SessionStore.class);
            when(sessionStore.isRevoked(pair.accessToken())).thenReturn(false);
            TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
            when(tenantLookupApi.findTenantStatus("TENANT1"))
                    .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", status)));

            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(
                    tokenFactory, sessionStore, tenantLookupApi);

            assertThrows(BizException.class,
                    () -> provider.authenticate(new JwtAuthenticationToken(new RawAccessJwtToken(pair.accessToken()))));
        }
    }

    @Test
    void allowsExpiredTenantAtAuthenticationTime() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = user("TENANT1");
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        SessionStore sessionStore = mock(SessionStore.class);
        when(sessionStore.isRevoked(pair.accessToken())).thenReturn(false);
        TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
        when(tenantLookupApi.findTenantStatus("TENANT1"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", "EXPIRED")));

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(
                tokenFactory, sessionStore, tenantLookupApi);

        JwtAuthenticationToken auth = (JwtAuthenticationToken) provider.authenticate(
                new JwtAuthenticationToken(new RawAccessJwtToken(pair.accessToken())));

        assertEquals("TENANT1", ((SecurityUser) auth.getPrincipal()).getTenantId());
    }

    @Test
    void toleratesMissingTenantLookupApi() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = user("TENANT1");
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        SessionStore sessionStore = mock(SessionStore.class);
        when(sessionStore.isRevoked(pair.accessToken())).thenReturn(false);

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(
                tokenFactory, sessionStore, null);

        JwtAuthenticationToken auth = (JwtAuthenticationToken) provider.authenticate(
                new JwtAuthenticationToken(new RawAccessJwtToken(pair.accessToken())));

        assertEquals("TENANT1", ((SecurityUser) auth.getPrincipal()).getTenantId());
    }

    @Test
    void reconstructsAuthoritiesFromTokenClaims() {
        JwtTokenFactory tokenFactory = newFactory();
        SecurityUser user = new SecurityUser(1L, "alice@example.com", "Alice", "hash", true, "TENANT1",
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "alice@example.com"),
                List.of(new SimpleGrantedAuthority("ROLE_admin"), new SimpleGrantedAuthority("system:user:list")));
        JwtPair pair = tokenFactory.createTokenPair(user, "TENANT1");
        SessionStore sessionStore = mock(SessionStore.class);
        when(sessionStore.isRevoked(pair.accessToken())).thenReturn(false);
        TenantLookupApi tenantLookupApi = mock(TenantLookupApi.class);
        when(tenantLookupApi.findTenantStatus("TENANT1"))
                .thenReturn(ApiResponse.ok(new TenantStatusView("TENANT1", "ENABLED")));

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(
                tokenFactory, sessionStore, tenantLookupApi);

        JwtAuthenticationToken auth = (JwtAuthenticationToken) provider.authenticate(
                new JwtAuthenticationToken(new RawAccessJwtToken(pair.accessToken())));

        assertEquals(List.of("ROLE_admin", "system:user:list"),
                ((SecurityUser) auth.getPrincipal()).getAuthorityStrings());
    }

    private static JwtTokenFactory newFactory() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setIssuer("rosecloud");
        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[64]));
        return new JwtTokenFactory(properties);
    }

    private static SecurityUser user(String tenantId) {
        return new SecurityUser(1L, "alice@example.com", "Alice", "hash", true, tenantId,
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "alice@example.com"), List.of());
    }
}
