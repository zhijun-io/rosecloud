package io.rosecloud.monolith.config;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.notice.service.NoticeService;
import io.rosecloud.system.domain.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonolithApiConfiguration {

    @Bean
    public NoticeRecipientApi noticeRecipientApi(UserRepository userRepository) {
        return request -> ApiResponse.ok(userRepository.findContacts(
                request.targetType(), request.targetTenantId(), request.targetRoleCode(), request.targetUsername()));
    }

    @Bean
    public NoticePublishApi noticePublishApi(NoticeService noticeService) {
        return request -> ApiResponse.ok(noticeService.publish(request));
    }
}
