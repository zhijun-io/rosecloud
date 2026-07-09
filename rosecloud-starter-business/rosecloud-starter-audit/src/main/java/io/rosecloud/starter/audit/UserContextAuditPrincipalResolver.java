package io.rosecloud.starter.audit;

import io.rosecloud.common.security.model.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Default {@link AuditPrincipalResolver}: reads the operator from
 * {@link SecurityContextHolder} (username from {@link SecurityUser}, falling
 * back to user id, then "anonymous"). Override with a custom
 * {@link AuditPrincipalResolver} bean for other sources.
 */
public class UserContextAuditPrincipalResolver implements AuditPrincipalResolver {

    @Override
    public String resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof SecurityUser securityUser)) {
            return "anonymous";
        }
        if (securityUser.getUsername() != null && !securityUser.getUsername().isBlank()) {
            return securityUser.getUsername();
        }
        return String.valueOf(securityUser.getUserId());
    }
}
