package io.rosecloud.api.notice;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign contract for publishing notices through the notice service.
 */
@FeignClient(name = "rosecloud-notice", contextId = "noticePublishApi", path = "/api/notice/notices")
public interface NoticePublishApi {

    @PostMapping
    ApiResponse<Long> publish(@RequestBody NoticePublishRequest request);
}
