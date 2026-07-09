package io.rosecloud.api.notice;

import io.rosecloud.common.core.model.ApiResponse;

/**
 * Internal contract for publishing notices through the notice service.
 * Transport-specific annotations live on {@link NoticePublishFeignApi}.
 */
public interface NoticePublishApi {

    ApiResponse<Long> publish(NoticePublishRequest request);
}
