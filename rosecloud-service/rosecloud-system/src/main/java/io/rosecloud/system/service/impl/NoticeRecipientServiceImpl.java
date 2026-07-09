package io.rosecloud.system.service.impl;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.system.domain.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeRecipientServiceImpl implements NoticeRecipientApi {

    private final UserRepository userRepository;

    public NoticeRecipientServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public ApiResponse<List<NoticeRecipient>> list(NoticeRecipientRequest request) {
        return ApiResponse.ok(userRepository.findContacts(
                request.targetType(), request.targetTenantId(), request.targetRoleCode(), request.targetUsername()));
    }
}
