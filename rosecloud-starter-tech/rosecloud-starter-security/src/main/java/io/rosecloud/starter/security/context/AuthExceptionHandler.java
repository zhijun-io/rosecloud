package io.rosecloud.starter.security.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthExceptionHandler extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public AuthExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            writeFailure(response, SecurityErrorCode.UNAUTHORIZED, e.getMessage());
        } catch (AccessDeniedException e) {
            writeFailure(response, SecurityErrorCode.FORBIDDEN, e.getMessage());
        }
    }

    private void writeFailure(HttpServletResponse response, SecurityErrorCode errorCode, String message)
            throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(errorCode.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.failure(errorCode.code(), message == null || message.isBlank()
                        ? errorCode.message()
                        : message));
    }
}
