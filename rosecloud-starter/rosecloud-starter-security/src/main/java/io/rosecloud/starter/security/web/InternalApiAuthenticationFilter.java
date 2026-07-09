package io.rosecloud.starter.security.web;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Gateway-trusted internal marker: when the {@code X-Internal} header is present (the
 * gateway is expected to strip it from external requests), the caller is treated as an
 * internal service and granted {@code ROLE_INTERNAL}. Internal endpoints guarded by
 * {@code @InternalApi} then pass their method-level {@code @PreAuthorize}.
 *
 * <p>No secret is verified here — trust is placed in the gateway/network boundary, which is
 * the Tier-A trade-off. For stronger guarantees use a signed internal header (Tier B).
 */
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_HEADER = "X-Internal";
    public static final String INTERNAL_AUTHORITY = "ROLE_INTERNAL";

    private static final String INTERNAL_PRINCIPAL = "internal-service";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String internal = request.getHeader(INTERNAL_HEADER);
        if (internal == null || internal.isBlank()) {
            filterChain.doFilter(request, response);
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
}
