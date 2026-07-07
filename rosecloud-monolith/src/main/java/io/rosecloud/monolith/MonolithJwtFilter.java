package io.rosecloud.monolith;

import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.starter.security.jwt.InvalidTokenException;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenClaims;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Monolith stand-in for the gateway: verifies the bearer JWT and injects the
 * decoded identity as {@link SecurityHeaders} on the request, so the shared
 * {@code SecurityContextFilter} populates {@code UserContext} exactly as it does
 * downstream of the gateway in microservice mode. Non-white-listed requests
 * without a valid, non-revoked token are rejected with 401, so logout (which
 * revokes the access-token jti) takes effect in-process.
 */
public class MonolithJwtFilter implements Filter {

    private final JwtTokenCodec jwtTokenCodec;
    private final TokenRevocationService tokenRevocationService;
    private final MonolithSecurityProperties properties;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    public MonolithJwtFilter(JwtTokenCodec jwtTokenCodec, TokenRevocationService tokenRevocationService,
                             MonolithSecurityProperties properties) {
        this.jwtTokenCodec = jwtTokenCodec;
        this.tokenRevocationService = tokenRevocationService;
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isWhiteListed(http.getRequestURI())) {
            chain.doFilter(http, response);
            return;
        }
        String token = extractToken(http);
        if (token == null) {
            unauthorized(httpResponse, "missing token");
            return;
        }
        TokenClaims claims;
        try {
            claims = jwtTokenCodec.parse(token);
        } catch (InvalidTokenException e) {
            unauthorized(httpResponse, "invalid token");
            return;
        }
        if (claims.jti() != null && tokenRevocationService.isRevoked(claims.jti())) {
            unauthorized(httpResponse, "token revoked");
            return;
        }
        chain.doFilter(new IdentityHeaderRequestWrapper(http, claims), response);
    }

    private boolean isWhiteListed(String path) {
        for (String pattern : properties.getWhiteList()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static String extractToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"code\":\"AUTHA003\",\"message\":\""
                + message + "\",\"data\":null}");
    }

    /**
     * Overrides the identity headers with the JWT-derived values (and leaves all
     * other headers untouched), so a client cannot spoof {@link SecurityHeaders}.
     */
    private static final class IdentityHeaderRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, List<String>> extra;

        IdentityHeaderRequestWrapper(HttpServletRequest request, TokenClaims claims) {
            super(request);
            Map<String, List<String>> headers = new HashMap<>();
            if (claims.userId() != null) {
                headers.put(SecurityHeaders.USER_ID, List.of(String.valueOf(claims.userId())));
            }
            if (claims.username() != null) {
                headers.put(SecurityHeaders.USERNAME, List.of(claims.username()));
            }
            if (claims.tenantId() != null) {
                headers.put(SecurityHeaders.TENANT_ID, List.of(String.valueOf(claims.tenantId())));
            }
            headers.put(SecurityHeaders.ROLES, List.of(String.join(",", claims.roles())));
            this.extra = headers;
        }

        @Override
        public String getHeader(String name) {
            if (extra.containsKey(name)) {
                return extra.get(name).get(0);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (extra.containsKey(name)) {
                return Collections.enumeration(extra.get(name));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> superNames = super.getHeaderNames();
            while (superNames.hasMoreElements()) {
                names.add(superNames.nextElement());
            }
            names.addAll(extra.keySet());
            return Collections.enumeration(names);
        }
    }
}
