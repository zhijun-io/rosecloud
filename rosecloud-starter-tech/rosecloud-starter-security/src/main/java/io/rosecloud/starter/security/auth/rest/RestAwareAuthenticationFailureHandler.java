package io.rosecloud.starter.security.auth.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.event.LoginFailedEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

public class RestAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

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
        String username = (String) request.getAttribute("ATTEMPTED_USERNAME");
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        eventPublisher.publishEvent(new LoginFailedEvent(username, null, e.getMessage(), ip, userAgent));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = switch (e) {
            case BadCredentialsException ignored -> "Bad credentials";
            case DisabledException ignored -> "User is disabled";
            case LockedException ignored -> "Account is locked";
            default -> "Authentication failed";
        };
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure("UNAUTHORIZED", message));
    }
}
