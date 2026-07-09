package io.rosecloud.api.notice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rosecloud-notice", contextId = "noticePublishApi", path = "/api/notice/notices")
public interface NoticePublishFeignApi extends NoticePublishApi {

    @Override
    @PostMapping
    Long publish(@RequestBody NoticePublishRequest request);
}
