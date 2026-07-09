package io.rosecloud.starter.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for audit logging.
 *
 * <p>Activated by {@code rosecloud.audit.enabled=true}; dormant otherwise. The
 * default principal resolver reads from {@code SecurityContextHolder}; the
 * aspect publishes {@link AuditLogEvent}s, and a listener logs them for
 * out-of-the-box observability.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.audit", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditPrincipalResolver auditPrincipalResolver() {
        return new UserContextAuditPrincipalResolver();
    }

    @Bean
    public AuditLogAspect auditLogAspect(ApplicationEventPublisher publisher, AuditPrincipalResolver resolver) {
        return new AuditLogAspect(publisher, resolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogEventListener auditLogEventListener() {
        return new AuditLogEventListener();
    }
}
