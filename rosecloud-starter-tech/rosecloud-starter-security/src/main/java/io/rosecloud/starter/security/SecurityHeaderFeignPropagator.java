package io.rosecloud.starter.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;

/**
 * Propagates the tenant context onto outbound Feign requests. Caller
 * identity is reconstructed from the bearer token on the receiving service, so
 * no user-id / username / role headers are forwarded.
 */
public class SecurityHeaderFeignPropagator implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        CurrentUser user = UserContext.get();
        if (user == null) {
            return;
        }
        if (user.tenantId() != null) {
            template.header(SecurityHeaders.TENANT_ID, String.valueOf(user.tenantId()));
        }
    }
}
