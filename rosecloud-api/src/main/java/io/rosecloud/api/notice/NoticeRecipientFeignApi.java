package io.rosecloud.api.notice;

import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "rosecloud-system", contextId = "noticeRecipientApi", path = "/internal/notice/recipients")
public interface NoticeRecipientFeignApi extends NoticeRecipientApi {

    @Override
    @PostMapping
    ApiResponse<List<NoticeRecipient>> list(@RequestBody NoticeRecipientRequest request);
}
