package io.rosecloud.starter.security.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.rosecloud.common.security.SecurityHeaders;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Propagates tenant id and the caller's raw JWT onto outbound Feign requests so the
 * downstream service can reconstruct {@link TenantContextHolder} and authenticate the caller.
 *
 * <p>The tenant id is always attached when present. The JWT is only attached when a
 * user context exists on the current thread (it is stashed in the {@code Authentication}
 * details by {@code JwtTokenAuthenticationProcessingFilter}); on async / machine-to-machine
 * calls (where the SecurityContext is not propagated) it is absent, and the downstream
 * endpoint is expected to be permitAll. A {@code X-Service-Name} header identifies the caller.
 *
 * <p>Registered as a bean so OpenFeign applies it to every Feign client. Only created
 * when both Feign and the tenant starter are on the classpath.
 */
public class ServiceAuthRequestInterceptor implements RequestInterceptor {

    private final String serviceName;

    public ServiceAuthRequestInterceptor(Environment environment) {
        this.serviceName = environment.getProperty("spring.application.name", "unknown");
    }

    @Override
    public void apply(RequestTemplate template) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            template.header(SecurityHeaders.TENANT_ID, tenantId);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof String rawToken) {
            template.header("Authorization", "Bearer " + rawToken);
        }
        template.header("X-Service-Name", serviceName);
    }
}
