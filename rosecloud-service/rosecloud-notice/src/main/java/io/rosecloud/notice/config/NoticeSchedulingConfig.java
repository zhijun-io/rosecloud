package io.rosecloud.notice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Registers the scheduled-notice publish sweep via {@link NoticeProperties} so
 * the interval is type-bound rather than a bare {@code @Scheduled} placeholder.
 * Spring's {@code @Scheduled} cannot read a bean value, so the task is added
 * programmatically with the same fixed-delay + initial-delay semantics.
 */
@Configuration
@EnableConfigurationProperties(NoticeProperties.class)
public class NoticeSchedulingConfig implements SchedulingConfigurer {

    private final NoticePublishScheduler scheduler;
    private final NoticeProperties properties;

    public NoticeSchedulingConfig(NoticePublishScheduler scheduler, NoticeProperties properties) {
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        long delay = properties.getPublishCheckMs();
        registrar.addFixedDelayTask(new IntervalTask(scheduler::publishDue, delay, delay));
    }
}
