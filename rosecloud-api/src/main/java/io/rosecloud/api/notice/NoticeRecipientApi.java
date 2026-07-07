package io.rosecloud.api.notice;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Feign contract for the notice service to resolve recipient contacts (email /
 * phone) from the system service for a notice target, so push channels can
 * deliver beyond the in-app station feed.
 */
@FeignClient(name = "rosecloud-system", contextId = "noticeRecipientApi", path = "/internal/notice/recipients")
public interface NoticeRecipientApi {

    @PostMapping
    ApiResponse<List<NoticeRecipient>> list(@RequestBody NoticeRecipientRequest request);
}
