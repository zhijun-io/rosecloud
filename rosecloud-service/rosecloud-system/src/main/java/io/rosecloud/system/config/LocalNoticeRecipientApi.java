package io.rosecloud.system.config;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.domain.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * In-process {@link NoticeRecipientApi} for monolith mode: delegates to
 * {@link UserRepository} instead of Feign.
 */
@Component
@ConditionalOnProperty(prefix = "spring.application", name = "name", havingValue = "rosecloud-monolith")
public class LocalNoticeRecipientApi implements NoticeRecipientApi {

    private final UserRepository userRepository;

    public LocalNoticeRecipientApi(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public ApiResponse<List<NoticeRecipient>> list(NoticeRecipientRequest request) {
        return ApiResponse.ok(userRepository.findContacts(
                request.targetType(), request.targetTenantId(), request.targetRoleCode()));
    }
}
