package io.rosecloud.notice.config;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.notice.service.NoticeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Local API adapter used when no Feign client bean is present.
 */
@Configuration
public class NoticeLocalApiConfiguration {

    @Bean
    @ConditionalOnMissingBean(NoticePublishApi.class)
    public NoticePublishApi noticePublishApi(NoticeService noticeService) {
        return request -> ApiResponse.ok(noticeService.publish(request));
    }
}
