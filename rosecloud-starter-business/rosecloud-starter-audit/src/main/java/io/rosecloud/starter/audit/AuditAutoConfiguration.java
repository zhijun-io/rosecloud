package io.rosecloud.starter.audit;

import io.rosecloud.api.audit.AuditLogApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@AutoConfiguration
@ConditionalOnProperty(prefix = "rosecloud.audit", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
public class AuditAutoConfiguration {

    @Bean
    public AuditLogAspect auditLogAspect(ApplicationEventPublisher publisher) {
        return new AuditLogAspect(publisher);
    }

    @Bean
    @ConditionalOnMissingBean(name = "auditLogExecutor")
    public ThreadPoolTaskExecutor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("audit-log-");
        executor.setBeanName("auditLogExecutor");
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogListener sysLogListener(AuditLogApi auditLogApi) {
        return new AuditLogListener(auditLogRequest -> {
            auditLogApi.save(auditLogRequest);
        });
    }

}
