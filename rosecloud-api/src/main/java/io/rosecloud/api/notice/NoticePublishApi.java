package io.rosecloud.api.notice;

import io.rosecloud.common.core.model.ApiResponse;

/**
 * Service contract for publishing notices.
 * Transport-specific annotations live on {@link NoticePublishFeignApi}.
 */
public interface NoticePublishApi {

    ApiResponse<Long> publish(NoticePublishRequest request);
}
