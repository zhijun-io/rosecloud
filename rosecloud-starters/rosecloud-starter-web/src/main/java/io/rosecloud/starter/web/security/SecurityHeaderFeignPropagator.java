package io.rosecloud.starter.web.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;

/**
 * Propagates the current {@link CurrentUser} onto outbound Feign requests as
 * {@link SecurityHeaders}, so downstream services see the same caller identity.
 */
public class SecurityHeaderFeignPropagator implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        CurrentUser user = UserContext.get();
        if (user == null) {
            return;
        }
        if (user.userId() != null) {
            template.header(SecurityHeaders.USER_ID, String.valueOf(user.userId()));
        }
        if (user.username() != null) {
            template.header(SecurityHeaders.USERNAME, user.username());
        }
        if (user.tenantId() != null) {
            template.header(SecurityHeaders.TENANT_ID, String.valueOf(user.tenantId()));
        }
        if (!user.roles().isEmpty()) {
            template.header(SecurityHeaders.ROLES, String.join(",", user.roles()));
        }
        if (user.traceId() != null) {
            template.header(SecurityHeaders.TRACE_ID, user.traceId());
        }
    }
}
