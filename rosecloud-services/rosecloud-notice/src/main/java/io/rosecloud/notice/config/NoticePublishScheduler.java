package io.rosecloud.notice.config;

import io.rosecloud.notice.service.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Flips scheduled notices to published once their publish time has passed.
 * Runs every minute; cheap because it is a single conditional update.
 */
@Component
public class NoticePublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoticePublishScheduler.class);

    private final NoticeService noticeService;

    public NoticePublishScheduler(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Scheduled(fixedDelayString = "${rosecloud.notice.publish-check-ms:60000}")
    public void publishDue() {
        int published = noticeService.publishScheduledNotices();
        if (published > 0) {
            log.info("published {} scheduled notice(s)", published);
        }
    }
}
