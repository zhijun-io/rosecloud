package io.rosecloud.notice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Provides a bounded executor for asynchronous notice dispatch. Dispatch runs
 * on a dedicated pool rather than the JVM-wide {@link java.util.concurrent.ForkJoinPool#commonPool()}
 * so a flood of outbound emails/SMS cannot saturate shared threads. The queue
 * is bounded and the rejection policy falls back to running the task on the
 * caller's thread, which keeps the publish sweep honest under load instead of
 * silently dropping deliveries.
 */
@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(NoticeProperties.class)
public class NoticeDispatchConfig {

    @Bean(name = "noticeDispatchExecutor")
    public Executor noticeDispatchExecutor(NoticeProperties properties) {
        NoticeProperties.Dispatch dispatch = properties.getDispatch();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(dispatch.getCorePoolSize());
        executor.setMaxPoolSize(dispatch.getMaxPoolSize());
        executor.setQueueCapacity(dispatch.getQueueCapacity());
        executor.setThreadNamePrefix("rosecloud-notice-dispatch-");
        executor.setRejectedExecutionHandler(callerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    private static RejectedExecutionHandler callerRunsPolicy() {
        return new ThreadPoolExecutor.CallerRunsPolicy();
    }
}
