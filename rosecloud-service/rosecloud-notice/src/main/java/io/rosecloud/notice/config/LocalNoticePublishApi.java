package io.rosecloud.notice.config;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.notice.service.NoticeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-process {@link NoticePublishApi} for monolith mode: delegates to
 * {@link NoticeService} instead of Feign.
 */
@Component
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "rosecloud-monolith")
public class LocalNoticePublishApi implements NoticePublishApi {

    private final NoticeService noticeService;

    public LocalNoticePublishApi(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Override
    public ApiResponse<Long> publish(NoticePublishRequest request) {
        return ApiResponse.ok(noticeService.publish(request));
    }
}
