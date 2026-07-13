package io.rosecloud.starter.security.auth.rest;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.util.JacksonUtil;
import io.rosecloud.common.security.event.LoginSucceededEvent;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.security.session.LoginSessionApi;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.starter.security.auth.LoginTenantResolver;
import io.rosecloud.starter.security.token.JwtTokenFactory;
import io.rosecloud.starter.security.util.DeviceFingerprint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
public class RestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenFactory tokenFactory;
    private final LoginSessionApi loginSessionApi;
    private final ApplicationEventPublisher eventPublisher;
    private final LoginTenantResolver loginTenantResolver;
    private final long refreshTokenExpirationSeconds;
    private final boolean tokenBindingEnabled;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        String activeTenantId = loginTenantResolver == null
                ? securityUser.getTenantId()
                : loginTenantResolver.resolveInitialTenant(securityUser);
        // M3: when device binding is enabled, embed a fingerprint of the client IP + UA so a
        // stolen token cannot be replayed from a different device. The same fingerprint is
        // used as the session/audit device id for device-trust.
        String deviceFingerprint = tokenBindingEnabled ? DeviceFingerprint.compute(request) : null;
        String deviceId = DeviceFingerprint.compute(request);
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser, activeTenantId, deviceFingerprint);

        String sessionId = UUID.randomUUID().toString();
        String token = tokenPair.accessToken();
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        Instant now = Instant.now();
        long expiresInSeconds = tokenFactory.getAccessTokenExpirationSeconds();
        Instant expireAt = now.plusSeconds(refreshTokenExpirationSeconds);
        loginSessionApi.save(new LoginSession(
                sessionId, token, tokenPair.refreshToken(), securityUser.getUserId(), securityUser.getUsername(),
                securityUser.getNickname(), ip, truncate(userAgent, 512), now, expireAt, deviceId));

        eventPublisher.publishEvent(new LoginSucceededEvent(securityUser, ip, truncate(userAgent, 512), deviceId));

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            JacksonUtil.getObjectMapper().writeValue(response.getWriter(), ApiResponse.ok(tokenPair));
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
