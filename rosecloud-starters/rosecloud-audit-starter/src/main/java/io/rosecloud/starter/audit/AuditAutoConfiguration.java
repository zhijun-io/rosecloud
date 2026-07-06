package io.rosecloud.starter.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for audit logging.
 *
 * <p>Activated by {@code rosecloud.audit.enabled=true}; dormant otherwise. Spring
 * Boot's AOP auto-configuration enables AspectJ proxying when
 * {@code spring-boot-starter-aop} is on the classpath.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.audit", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditPrincipalResolver auditPrincipalResolver() {
        return () -> "unknown";
    }

    @Bean
    public AuditLogAspect auditLogAspect(ApplicationEventPublisher publisher, AuditPrincipalResolver resolver) {
        return new AuditLogAspect(publisher, resolver);
    }
}
