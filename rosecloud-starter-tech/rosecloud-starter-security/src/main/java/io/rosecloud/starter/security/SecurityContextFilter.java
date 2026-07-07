package io.rosecloud.starter.security;

import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.security.jwt.InvalidTokenException;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenClaims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.List;

/**
 * Decodes the bearer JWT into a {@link CurrentUser} bound to {@link UserContext}
 * for the request duration. Identity no longer comes from client-controlled
 * headers. The trace id is expected to be provided by the dedicated trace
 * starter before this filter runs.
 */
public class SecurityContextFilter implements Filter {

    private final JwtTokenCodec jwtTokenCodec;

    public SecurityContextFilter(JwtTokenCodec jwtTokenCodec) {
        this.jwtTokenCodec = jwtTokenCodec;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        UserContext.set(decode(http));
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private CurrentUser decode(HttpServletRequest request) {
        String token = extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION));
        String traceId = request.getHeader(SecurityHeaders.TRACE_ID);
        if (token == null) {
            return new CurrentUser(null, null, null, List.of(), traceId);
        }
        try {
            TokenClaims claims = jwtTokenCodec.parse(token);
            return new CurrentUser(claims.userId(), claims.username(), claims.tenantId(), claims.roles(), traceId);
        } catch (InvalidTokenException e) {
            return new CurrentUser(null, null, null, List.of(), traceId);
        }
    }

    private static String extractBearer(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
