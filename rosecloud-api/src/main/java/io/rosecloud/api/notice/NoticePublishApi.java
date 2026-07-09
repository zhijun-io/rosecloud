package io.rosecloud.api.notice;

/**
 * Service contract for publishing notices.
 * Transport-specific annotations live on {@link NoticePublishFeignApi}.
 */
public interface NoticePublishApi {

    Long publish(NoticePublishRequest request);
}
