package io.rosecloud.starter.audit;

import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

/**
 * Around-advises {@link AuditLog} methods, timing them and publishing an
 * {@link AuditLogEvent} regardless of outcome. The operator and tenant are
 * read from {@link UserContext}.
 */
@Aspect
public class AuditLogAspect {

    private final ApplicationEventPublisher publisher;
    private final AuditPrincipalResolver principalResolver;

    public AuditLogAspect(ApplicationEventPublisher publisher, AuditPrincipalResolver principalResolver) {
        this.publisher = publisher;
        this.principalResolver = principalResolver;
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
            publisher.publishEvent(new AuditLogEvent(
                    action,
                    auditLog.description(),
                    principalResolver.resolve(),
                    currentTenantId(),
                    pjp.getSignature().toShortString(),
                    elapsed,
                    Instant.now(),
                    failure
            ));
        }
    }

    private static Long currentTenantId() {
        CurrentUser user = UserContext.get();
        return user == null ? null : user.tenantId();
    }
}
