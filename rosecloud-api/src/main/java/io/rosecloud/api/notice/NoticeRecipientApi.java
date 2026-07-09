package io.rosecloud.api.notice;

import io.rosecloud.common.core.model.ApiResponse;

import java.util.List;

/**
 * Internal contract for resolving recipient contacts (email / phone) from the
 * system service for a notice target. Transport-specific annotations live on
 * {@link NoticeRecipientFeignApi}.
 */
public interface NoticeRecipientApi {

    ApiResponse<List<NoticeRecipient>> list(NoticeRecipientRequest request);
}
