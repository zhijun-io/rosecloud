package io.rosecloud.starter.security.web;

import io.rosecloud.starter.security.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal-service trust boundary. Services authenticate each other by presenting a
 * shared {@code X-Internal} token (set by the Feign {@code ServiceAuthRequestInterceptor});
 * a request carrying a valid token is granted {@code ROLE_INTERNAL} so that endpoints guarded
 * by {@code @InternalApi} pass their method-level {@code @PreAuthorize}.
 *
 * <p>The token value is verified (constant-time) against {@code rosecloud.security.internal-token};
 * a present-but-invalid token is rejected. External clients can never supply a valid token because
 * the gateway strips {@code X-Internal} from every inbound request. This replaces the previous
 * trust-on-presence model, which any client could spoof by simply adding the header.
 */
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_HEADER = "X-Internal";
    public static final String INTERNAL_AUTHORITY = "ROLE_INTERNAL";

    private static final String INTERNAL_PRINCIPAL = "internal-service";

    private final String internalToken;

    public InternalApiAuthenticationFilter(SecurityProperties properties) {
        String token = properties.getInternalToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "rosecloud.security.internal-token must be set (env ROSECLOUD_SECURITY_INTERNAL_TOKEN). "
                            + "Refusing to start with an empty internal token.");
        }
        this.internalToken = token;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String internal = request.getHeader(INTERNAL_HEADER);
        if (internal == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!constantTimeEquals(internal, internalToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid internal token");
            return;
        }

        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() == null || !context.getAuthentication().isAuthenticated()) {
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    INTERNAL_PRINCIPAL, null, List.of(new SimpleGrantedAuthority(INTERNAL_AUTHORITY))));
        } else if (CollectionUtils.isEmpty(context.getAuthentication().getAuthorities())
                || context.getAuthentication().getAuthorities().stream()
                .noneMatch(a -> INTERNAL_AUTHORITY.equals(a.getAuthority()))) {
            List<GrantedAuthority> authorities = new ArrayList<>(context.getAuthentication().getAuthorities());
            authorities.add(new SimpleGrantedAuthority(INTERNAL_AUTHORITY));
            UsernamePasswordAuthenticationToken augmented = new UsernamePasswordAuthenticationToken(
                    context.getAuthentication().getPrincipal(),
                    context.getAuthentication().getCredentials(),
                    authorities);
            context.setAuthentication(augmented);
        }

        filterChain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        // MessageDigest.isEqual performs a length-independent constant-time comparison,
        // avoiding the previous early length-leak that revealed the expected token length.
        return MessageDigest.isEqual(ab, bb);
    }
}
