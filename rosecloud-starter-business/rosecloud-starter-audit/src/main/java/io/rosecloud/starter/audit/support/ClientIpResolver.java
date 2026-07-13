package io.rosecloud.starter.audit.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the originating client IP from the current request, honoring proxy
 * headers ({@code X-Forwarded-For}, {@code X-Real-IP}) before falling back to
 * {@link HttpServletRequest#getRemoteAddr()}. Returns {@code null} when no
 * request context is bound (e.g. non-servlet or async thread without decoration).
 */
public final class ClientIpResolver {

    private static final String[] FORWARDING_HEADERS = {"X-Forwarded-For", "X-Real-IP"};

    private ClientIpResolver() {
    }

    public static String resolve() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            return null;
        }
        for (String header : FORWARDING_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
