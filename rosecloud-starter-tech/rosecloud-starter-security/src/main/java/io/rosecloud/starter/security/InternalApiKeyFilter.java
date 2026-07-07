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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
        if (provided == null || !constantTimeEquals(provided, expectedKey)) {
            unauthorized(httpResponse, "internal access requires a valid api key");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        ErrorJson.write(response, HttpStatus.UNAUTHORIZED.value(), SecurityErrorCode.INTERNAL_API_KEY_INVALID, message);
    }
}
