package io.rosecloud.starter.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import org.springframework.http.HttpHeaders;

/**
 * Propagates caller identity onto outbound Feign requests.
 *
 * <ul>
 *   <li>Business calls ({@code /api/**}) forward the inbound bearer token so the
 *       downstream service reconstructs the real caller via its security filter.
 *   <li>Internal service-to-service calls ({@code /internal/**}) use the shared
 *       {@link SecurityHeaders#INTERNAL_API_KEY} instead of a user bearer. This
 *       prevents a caller's token from being replayed against the identity
 *       resolution endpoint and avoids an authentication loop.
 * </ul>
 */
public class SecurityHeaderFeignPropagator implements RequestInterceptor {

    private final String internalApiKey;

    public SecurityHeaderFeignPropagator(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (isInternalPath(template)) {
            template.header(SecurityHeaders.INTERNAL_API_KEY, internalApiKey);
            return;
        }
        CurrentUser user = UserContext.get();
        if (user != null && user.tenantId() != null) {
            template.header(SecurityHeaders.TENANT_ID, String.valueOf(user.tenantId()));
        }
        String token = UserContext.getToken();
        if (token != null) {
            template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
    }

    private static boolean isInternalPath(RequestTemplate template) {
        String url = template.url();
        if (url != null && url.contains("/internal/")) {
            return true;
        }
        String path = template.path();
        return path != null && path.contains("/internal/");
    }
}
