package io.rosecloud.starter.audit;

import io.rosecloud.api.audit.AuditLogApi;
import io.rosecloud.api.audit.AuditLogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for audit logging.
 *
 * <p>Activated by {@code rosecloud.audit.enabled=true}. Publishes {@link AuditLogRequest}s
 * via {@link AuditLogAspect}; consumers persist them:
 * <ul>
 *   <li><b>Monolith / co-deploy:</b> {@code AuditLogPersistenceListener} (rosecloud-system)
 *       receives events in the same Spring context and writes to the DB.</li>
 *   <li><b>Microservices (notice):</b> the same listener (or a remote counterpart) calls
 *       {@code AuditLogRemoteSaver} (Feign) to forward events to the system service.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.audit", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
public class AuditAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AuditAutoConfiguration.class);

    @Bean
    public AuditLogAspect auditLogAspect(ApplicationEventPublisher publisher) {
        return new AuditLogAspect(publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogListener sysLogListener(AuditLogApi auditLogApi) {
        return new AuditLogListener(auditLogRequest -> {
            log.info("auditLogRequest: {}", auditLogRequest);
            auditLogApi.save(auditLogRequest);
        });
    }

}
