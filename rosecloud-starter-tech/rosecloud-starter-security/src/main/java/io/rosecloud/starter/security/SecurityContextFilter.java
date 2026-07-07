package io.rosecloud.starter.security;

import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.ApiResponse;
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
 * for the request duration. The token only carries the unique username; the
 * full user snapshot is resolved from the system user source after validation.
 */
public class SecurityContextFilter implements Filter {

    private final JwtTokenCodec jwtTokenCodec;
    private final SystemUserApi systemUserApi;

    public SecurityContextFilter(JwtTokenCodec jwtTokenCodec, SystemUserApi systemUserApi) {
        this.jwtTokenCodec = jwtTokenCodec;
        this.systemUserApi = systemUserApi;
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
        if (token == null) {
            return new CurrentUser(null, null, null, List.of());
        }
        try {
            TokenClaims claims = jwtTokenCodec.parse(token);
            ApiResponse<UserAuthInfo> response = systemUserApi.getAuthInfo(claims.username());
            UserAuthInfo user = response.success() ? response.data() : null;
            if (user == null) {
                return new CurrentUser(null, claims.username(), null, List.of());
            }
            return new CurrentUser(user.userId(), user.username(), user.tenantId(), user.roles());
        } catch (InvalidTokenException e) {
            return new CurrentUser(null, null, null, List.of());
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
