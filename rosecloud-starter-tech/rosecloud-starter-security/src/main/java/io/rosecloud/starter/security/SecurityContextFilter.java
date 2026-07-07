package io.rosecloud.starter.security;

import io.rosecloud.api.user.SystemUserApi;
import io.rosecloud.api.user.UserAuthInfo;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.starter.security.jwt.InvalidTokenException;
import io.rosecloud.starter.security.jwt.JwtTokenCodec;
import io.rosecloud.starter.security.jwt.TokenClaims;
import io.rosecloud.starter.security.jwt.TokenType;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Decodes the bearer JWT into a {@link CurrentUser} bound to {@link UserContext}
 * for the request duration, and mirrors it into the Spring Security
 * {@link SecurityContextHolder} so endpoint-level {@code @PreAuthorize} rules are
 * enforced. The token only carries the unique username; the full user snapshot
 * is resolved from the system user source after validation.
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
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        DecodeResult result = decode(http);
        if (result.rejected()) {
            unauthorized(httpResponse, result.message());
            return;
        }
        UserContext.set(result.user());
        if (result.user().username() != null) {
            List<GrantedAuthority> authorities = Stream.concat(
                            result.user().roles().stream(), result.user().perms().stream())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            result.user().username(), null, authorities));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private DecodeResult decode(HttpServletRequest request) {
        String token = extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return DecodeResult.anonymous();
        }
        UserContext.setToken(token);
        try {
            TokenClaims claims = jwtTokenCodec.parse(token);
            if (claims.type() != TokenType.ACCESS) {
                return DecodeResult.reject("wrong token type");
            }
            ApiResponse<UserAuthInfo> response = systemUserApi.getAuthInfo(claims.username());
            UserAuthInfo user = response.success() ? response.data() : null;
            if (user == null) {
                return DecodeResult.reject("unknown user");
            }
            return DecodeResult.user(new CurrentUser(user.userId(), user.username(), user.tenantId(),
                    user.roles(), claims.perms()));
        } catch (InvalidTokenException e) {
            return DecodeResult.reject("invalid token");
        }
    }

    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"code\":\"AUTHA003\",\"message\":\""
                + message + "\",\"data\":null}");
    }

    private static String extractBearer(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    /**
     * Result of decoding the request bearer token. A rejected result means a
     * token was present but unusable (wrong type, invalid, or unknown user) and
     * the request must be refused with 401 rather than served as anonymous.
     */
    private record DecodeResult(CurrentUser user, boolean rejected, String message) {

        static DecodeResult anonymous() {
            return new DecodeResult(new CurrentUser(null, null, null, List.of(), List.of()), false, null);
        }

        static DecodeResult user(CurrentUser user) {
            return new DecodeResult(user, false, null);
        }

        static DecodeResult reject(String message) {
            return new DecodeResult(new CurrentUser(null, null, null, List.of(), List.of()), true, message);
        }
    }
}
