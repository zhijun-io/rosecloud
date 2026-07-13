package io.rosecloud.starter.audit;

import io.rosecloud.api.audit.AuditLogRequest;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.audit.support.ClientIpResolver;
import io.rosecloud.starter.tenant.core.TenantContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

/**
 * Around-advises {@link AuditLog} methods, timing them and publishing an
 * {@link AuditLogRequest} regardless of outcome.
 */
@Aspect
public class AuditLogAspect {

    private static final ExpressionParser SPEL = new SpelExpressionParser();

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
            boolean success = failure == null;
            String action = auditLog.action().isBlank()
                    ? pjp.getSignature().toShortString()
                    : auditLog.action();
            String entityType = auditLog.entityType().isBlank() ? null : auditLog.entityType();
            String entityId = entityType == null ? null : resolveExpression(auditLog.entityId(), pjp);
            publisher.publishEvent(new AuditLogRequest(
                    action,
                    auditLog.description(),
                    resolvePrincipal(),
                    currentTenantId(),
                    pjp.getSignature().toShortString(),
                    elapsed,
                    success,
                    failure != null ? failure.getMessage() : null,
                    LocalDateTime.now(),
                    entityType,
                    entityId,
                    ClientIpResolver.resolve(),
                    resolveSeverity(auditLog.severity(), success)
            ));
        }
    }

    static String resolveSeverity(String configured, boolean success) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return success ? "INFO" : "ERROR";
    }

    private static String resolveExpression(String expression, ProceedingJoinPoint pjp) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        return resolveExpression(expression, signature.getParameterNames(), pjp.getArgs());
    }

    static String resolveExpression(String expression, String[] paramNames, Object[] args) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            Object value = SPEL.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static String currentTenantId() {
        return TenantContextHolder.getTenantId();
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
