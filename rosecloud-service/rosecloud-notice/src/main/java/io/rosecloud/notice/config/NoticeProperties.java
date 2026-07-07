package io.rosecloud.notice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Notice service configuration under {@code rosecloud.notice.*}.
 */
@ConfigurationProperties(prefix = "rosecloud.notice")
public class NoticeProperties {

    /** Fixed delay (ms) between scheduled-notice publish sweeps. */
    private long publishCheckMs = 60000;

    /**
     * Bounds the async dispatch executor so a burst of outbound notices cannot
     * grow an unbounded common-pool and starve the rest of the JVM. When the
     * queue is saturated the submitting thread runs the task itself (CallerRuns),
     * applying natural backpressure instead of dropping deliveries.
     */
    private final Dispatch dispatch = new Dispatch();

    public long getPublishCheckMs() {
        return publishCheckMs;
    }

    public void setPublishCheckMs(long publishCheckMs) {
        this.publishCheckMs = publishCheckMs;
    }

    public Dispatch getDispatch() {
        return dispatch;
    }

    /** Tunables for the notice dispatch thread pool. */
    public static class Dispatch {
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private int queueCapacity = 64;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }
}
