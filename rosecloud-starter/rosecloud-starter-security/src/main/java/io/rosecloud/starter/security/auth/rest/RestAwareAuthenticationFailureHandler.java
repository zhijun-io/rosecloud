package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.event.LoginFailedEvent;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

        eventPublisher.publishEvent(new LoginFailedEvent(username, null, e.getMessage(), ip, userAgent));

        SecurityErrorCode errorCode = errorCode(e);
        response.setStatus(errorCode.httpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.failure(errorCode.code(), message(e, errorCode)));
    }

    private SecurityErrorCode errorCode(AuthenticationException e) {
        return switch (e) {
            case BadCredentialsException ignored -> SecurityErrorCode.BAD_CREDENTIALS;
            case DisabledException ignored -> SecurityErrorCode.USER_DISABLED;
            case LockedException ignored -> SecurityErrorCode.UNAUTHORIZED;
            case UsernameNotFoundException ignored -> SecurityErrorCode.USER_NOT_FOUND;
            default -> SecurityErrorCode.UNAUTHORIZED;
        };
    }

    private String message(AuthenticationException e, SecurityErrorCode errorCode) {
        return switch (e) {
            case BadCredentialsException ignored -> errorCode.message();
            case DisabledException ignored -> errorCode.message();
            case LockedException ignored -> "Account is locked";
            case UsernameNotFoundException ignored -> errorCode.message();
            default -> errorCode.message();
        };
    }
}
