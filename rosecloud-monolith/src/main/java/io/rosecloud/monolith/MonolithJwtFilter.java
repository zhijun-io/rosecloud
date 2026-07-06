package io.rosecloud.monolith;

import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.starter.security.jwt.InvalidTokenException;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenClaims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
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
 * {@code SecurityContextFilter} populates {@code UserContext} exactly as it
 * does downstream of the gateway in microservice mode. Requests without a
 * valid token are treated as anonymous.
 */
public class MonolithJwtFilter implements Filter {

    private final JwtTokenCodec jwtTokenCodec;

    public MonolithJwtFilter(JwtTokenCodec jwtTokenCodec) {
        this.jwtTokenCodec = jwtTokenCodec;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String token = extractToken(http);
        if (token != null) {
            try {
                TokenClaims claims = jwtTokenCodec.parse(token);
                http = new IdentityHeaderRequestWrapper(http, claims);
            } catch (InvalidTokenException ignored) {
                // invalid token: proceed anonymously (no identity headers)
            }
        }
        chain.doFilter(http, response);
    }

    private static String extractToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

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
