package io.rosecloud.starter.audit;

import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.tenant.core.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

/**
 * Around-advises {@link AuditLog} methods, timing them and publishing an
 * {@link AuditLogRequest} regardless of outcome.
 */
@Aspect
public class AuditLogAspect {

    private final ApplicationEventPublisher publisher;

    public AuditLogAspect(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable failure = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String action = auditLog.action().isBlank()
                    ? pjp.getSignature().toShortString()
                    : auditLog.action();
            publisher.publishEvent(new AuditLogRequest(
                    action,
                    auditLog.description(),
                    resolvePrincipal(),
                    currentTenantId(),
                    pjp.getSignature().toShortString(),
                    elapsed,
                    failure != null,
                    failure.getMessage(),
                    LocalDateTime.now()
            ));
        }
    }

    private static String currentTenantId() {
        return TenantContext.getTenantId();
    }

    private static String resolvePrincipal() {
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
