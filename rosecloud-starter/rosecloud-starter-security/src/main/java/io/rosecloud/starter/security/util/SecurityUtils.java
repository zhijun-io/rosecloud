package io.rosecloud.starter.security.util;
import io.rosecloud.starter.security.token.BearerTokenExtractor;

import io.rosecloud.common.security.model.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static helpers for extracting common security and request context
 * without depending on rosecloud-api or spreading boilerplate.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Returns the authenticated {@link SecurityUser} from the current security context,
     * or {@code null} when no authentication is available or the principal is not a
     * {@code SecurityUser}.
     */
    @Nullable
    public static SecurityUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof SecurityUser su) {
            return su;
        }
        return null;
    }

    /**
     * Returns the authenticated {@link SecurityUser} from the current security context,
     * throwing {@link AuthenticationCredentialsNotFoundException} when absent.
     */
    public static SecurityUser getRequiredCurrentUser() {
        SecurityUser user = getCurrentUser();
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user found");
        }
        return user;
    }

    /**
     * Returns the client IP address from the request, falling back to
     * {@code "0.0.0.0"} when the remote address is null.
     * Checks the {@code X-Forwarded-For} header first for proxy-aware resolution.
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "0.0.0.0";
    }

    /**
     * Returns the User-Agent header, falling back to an empty string when absent.
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "";
    }

    /**
     * Returns the Bearer token extracted from the current request via
     * {@link BearerTokenExtractor}. Returns {@code null} when the header is missing.
     */
    @Nullable
    public static String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        try {
            return new BearerTokenExtractor().extract(request);
        } catch (Exception e) {
            return null;
        }
    }
}
