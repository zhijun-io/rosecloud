package io.rosecloud.notice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Notice service configuration under {@code rosecloud.notice.*}.
 */
@ConfigurationProperties(prefix = "rosecloud.notice")
public class NoticeProperties {

    /** Fixed delay (ms) between scheduled-notice publish sweeps. */
    private long publishCheckMs = 60000;

    public long getPublishCheckMs() {
        return publishCheckMs;
    }

    public void setPublishCheckMs(long publishCheckMs) {
        this.publishCheckMs = publishCheckMs;
    }
}
