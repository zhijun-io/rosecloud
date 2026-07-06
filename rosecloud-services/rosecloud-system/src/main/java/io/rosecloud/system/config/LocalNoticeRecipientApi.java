package io.rosecloud.system.config;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.domain.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * In-process {@link NoticeRecipientApi} for monolith mode: delegates to
 * {@link UserRepository} instead of Feign. Only active under the
 * {@code monolith} profile.
 */
@Profile("monolith")
@Component
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
