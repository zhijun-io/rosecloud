package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.model.UserPrincipal;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.starter.security.auth.LoginTenantResolver;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestAwareAuthenticationSuccessHandlerTest {

    @Test
    void loginUsesResolvedTenantWhenAvailable() throws Exception {
        JwtTokenFactory tokenFactory = mock(JwtTokenFactory.class);
        SessionStore sessionStore = mock(SessionStore.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        LoginTenantResolver tenantResolver = mock(LoginTenantResolver.class);
        when(tenantResolver.resolveInitialTenant(any())).thenReturn("TENANT2");
        when(tokenFactory.createTokenPair(any(), eq("TENANT2"), any())).thenReturn(new JwtPair("access-1", "refresh-1"));
        when(tokenFactory.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        RestAwareAuthenticationSuccessHandler handler = new RestAwareAuthenticationSuccessHandler(
                tokenFactory, sessionStore, eventPublisher, new ObjectMapper(), tenantResolver, 86400L, false);

        SecurityUser securityUser = new SecurityUser(1L, "alice@example.com", "Alice", "hash", true, "TENANT1",
                new UserPrincipal(UserPrincipal.Type.USER_NAME, "alice@example.com"), List.of());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(securityUser);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(tokenFactory).createTokenPair(any(), eq("TENANT2"), any());
        verify(sessionStore).save(any(LoginSession.class));
        assertEquals(200, response.getStatus());
    }
}
