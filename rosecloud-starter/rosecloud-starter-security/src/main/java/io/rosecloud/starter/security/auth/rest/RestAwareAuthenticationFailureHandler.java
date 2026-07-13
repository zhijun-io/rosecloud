package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.event.LoginFailedEvent;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import io.rosecloud.starter.security.util.DeviceFingerprint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RestAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAwareAuthenticationFailureHandler.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public RestAwareAuthenticationFailureHandler(ApplicationEventPublisher eventPublisher,
                                                 ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException {
        log.warn("Authentication failure: {}", e.getMessage());

        String username = (String) request.getAttribute("ATTEMPTED_USERNAME");
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String deviceId = DeviceFingerprint.compute(request);

        eventPublisher.publishEvent(new LoginFailedEvent(username, null, e.getMessage(), ip, userAgent, deviceId));

        // A locked account (H3) surfaces a distinct 423 so a legitimate user knows why they
        // are blocked; every other failure stays a uniform 401 to avoid username enumeration.
        SecurityErrorCode errorCode = (e instanceof LockedException)
                ? SecurityErrorCode.ACCOUNT_LOCKED
                : SecurityErrorCode.BAD_CREDENTIALS;
        response.setStatus(errorCode.httpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.failure(errorCode.code(), errorCode.message()));
    }
}
