package io.rosecloud.system.task;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables {@code @Async} task execution and provides a dedicated thread pool
 * ({@code rosecloudTaskExecutor}). v1 runs tasks in-process (single-instance);
 * distribution via a broker is a follow-up.
 */
@Configuration
@EnableAsync
public class TaskAsyncConfiguration {

    @Bean(name = "rosecloudTaskExecutor")
    public ThreadPoolTaskExecutor rosecloudTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("task-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
