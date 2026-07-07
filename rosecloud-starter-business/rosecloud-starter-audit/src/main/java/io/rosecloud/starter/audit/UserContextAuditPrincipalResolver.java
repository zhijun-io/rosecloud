package io.rosecloud.starter.audit;

import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;

/**
 * Default {@link AuditPrincipalResolver}: reads the operator from
 * {@link UserContext} (username, falling back to user id, then "anonymous").
 * Override with a custom {@link AuditPrincipalResolver} bean for other sources.
 */
public class UserContextAuditPrincipalResolver implements AuditPrincipalResolver {

    @Override
    public String resolve() {
        CurrentUser user = UserContext.get();
        if (user == null) {
            return "anonymous";
        }
        if (user.username() != null && !user.username().isBlank()) {
            return user.username();
        }
        return user.userId() == null ? "anonymous" : String.valueOf(user.userId());
    }
}
