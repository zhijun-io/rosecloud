package io.rosecloud.notice.config;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.notice.service.NoticeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Local API adapter for the monolith profile.
 */
@Configuration
@Profile("monolith")
public class NoticeLocalApiConfiguration {

    @Bean
    public NoticePublishApi noticePublishApi(NoticeService noticeService) {
        return request -> ApiResponse.ok(noticeService.publish(request));
    }
}
