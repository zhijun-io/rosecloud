package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.security.event.LoginSucceededEvent;
import io.rosecloud.common.security.model.LoginSession;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.common.security.session.SessionStore;
import io.rosecloud.common.security.token.JwtPair;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.starter.security.token.JwtTokenFactory;
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

public class RestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenFactory tokenFactory;
    private final SessionStore sessionStore;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public RestAwareAuthenticationSuccessHandler(JwtTokenFactory tokenFactory,
                                                 SessionStore sessionStore,
                                                 ApplicationEventPublisher eventPublisher,
                                                 ObjectMapper objectMapper) {
        this.tokenFactory = tokenFactory;
        this.sessionStore = sessionStore;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        JwtPair tokenPair = tokenFactory.createTokenPair(securityUser);

        String sessionId = UUID.randomUUID().toString();
        String token = tokenPair.accessToken();
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        Instant now = Instant.now();
        long expiresInSeconds = tokenFactory.getAccessTokenExpirationSeconds();
        Instant expireAt = now.plusSeconds(expiresInSeconds);
        sessionStore.save(new LoginSession(
                sessionId, token, securityUser.getUserId(), securityUser.getUsername(),
                securityUser.getNickname(), ip, truncate(userAgent, 512), now, expireAt));

        eventPublisher.publishEvent(new LoginSucceededEvent(securityUser, ip, truncate(userAgent, 512)));

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.ok(tokenPair));
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
