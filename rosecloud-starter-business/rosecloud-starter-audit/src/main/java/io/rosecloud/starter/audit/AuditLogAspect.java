package io.rosecloud.starter.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

/**
 * Around-advises {@link AuditLog} methods, timing them and publishing an
 * {@link AuditLogEvent} regardless of outcome. The operator and tenant are
 * read from {@link SecurityContextHolder}.
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

    private static String currentTenantId() {
        // Tenant ID is resolved by the tenant module, not stored in SecurityUser.
        // Services that need tenant tracking provide their own AuditPrincipalResolver.
        return null;
    }
}
