package io.rosecloud.starter.security;

import io.rosecloud.common.security.SecurityHeaders;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Guards {@code /internal/**} endpoints. A request must carry the shared
 * {@link SecurityHeaders#INTERNAL_API_KEY} to be served; anything else (including
 * a valid user bearer) is refused with 401. This keeps service-to-service
 * endpoints - which can return password hashes and other sensitive snapshots -
 * off-limits to external callers.
 */
public class InternalApiKeyFilter implements Filter {

    private final String expectedKey;

    public InternalApiKeyFilter(String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String provided = http.getHeader(SecurityHeaders.INTERNAL_API_KEY);
        if (provided == null || !provided.equals(expectedKey)) {
            unauthorized(httpResponse, "internal access requires a valid api key");
            return;
        }
        chain.doFilter(request, response);
    }

    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"code\":\"" + SecurityErrorCode.INTERNAL_API_KEY_INVALID.code() + "\",\"message\":\""
                + message + "\",\"data\":null}");
    }
}
