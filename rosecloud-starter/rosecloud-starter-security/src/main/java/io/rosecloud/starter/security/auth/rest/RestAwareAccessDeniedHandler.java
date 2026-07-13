package io.rosecloud.starter.security.auth.rest;
import lombok.RequiredArgsConstructor;

import io.rosecloud.common.core.util.JacksonUtil;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.exception.SecurityErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class RestAwareAccessDeniedHandler implements AccessDeniedHandler {


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(SecurityErrorCode.FORBIDDEN.httpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JacksonUtil.getObjectMapper().writeValue(response.getWriter(),
                ApiResponse.failure(SecurityErrorCode.FORBIDDEN.code(), SecurityErrorCode.FORBIDDEN.message()));
    }
}
