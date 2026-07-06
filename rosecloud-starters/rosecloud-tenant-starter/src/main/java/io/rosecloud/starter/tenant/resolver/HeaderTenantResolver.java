package io.rosecloud.starter.tenant.resolver;

import io.rosecloud.starter.tenant.core.TenantProperties;
import jakarta.servlet.http.HttpServletRequest;

/** Default resolver: reads the tenant id from a configured request header. */
public class HeaderTenantResolver implements TenantResolver {

    private final TenantProperties properties;

    public HeaderTenantResolver(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Long resolve(HttpServletRequest request) {
        return parse(request.getHeader(properties.getHeaderName()));
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
