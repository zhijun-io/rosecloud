package io.rosecloud.monolith;

import io.rosecloud.starter.security.PublicPathsProperties;
import io.rosecloud.starter.security.jwt.InvalidTokenException;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenClaims;
import io.rosecloud.starter.security.jwt.TokenType;
import io.rosecloud.starter.security.jwt.TokenRevocationService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Monolith stand-in for the gateway: verifies the bearer JWT before the shared
 * trace and security filters run. Non-white-listed requests without a valid,
 * non-revoked token are rejected with 401, so logout (which revokes the
 * access-token jti) takes effect in-process.
 */
public class MonolithJwtFilter implements Filter {

    private final JwtTokenCodec jwtTokenCodec;
    private final TokenRevocationService tokenRevocationService;
    private final PublicPathsProperties properties;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    public MonolithJwtFilter(JwtTokenCodec jwtTokenCodec, TokenRevocationService tokenRevocationService,
                             PublicPathsProperties properties) {
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
        if (claims.type() != TokenType.ACCESS) {
            unauthorized(httpResponse, "wrong token type");
            return;
        }
        if (claims.jti() != null && tokenRevocationService.isRevoked(claims.jti())) {
            unauthorized(httpResponse, "token revoked");
            return;
        }
        chain.doFilter(http, response);
    }

    private boolean isWhiteListed(String path) {
        for (String pattern : properties.getPublicPaths()) {
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

}
