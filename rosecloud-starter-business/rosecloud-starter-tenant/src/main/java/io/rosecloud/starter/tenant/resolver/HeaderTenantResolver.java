package io.rosecloud.starter.tenant.resolver;

import io.rosecloud.common.security.SecurityHeaders;
import jakarta.servlet.http.HttpServletRequest;

/** Default resolver: reads the tenant id from {@link SecurityHeaders#TENANT_ID}. */
public class HeaderTenantResolver implements TenantResolver {

    @Override
    public Long resolve(HttpServletRequest request) {
        return parse(request.getHeader(SecurityHeaders.TENANT_ID));
    }

    static Long parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
