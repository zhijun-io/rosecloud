package io.rosecloud.system.controller;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.ServiceMetadata;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * System endpoints for resolving recipient contacts and Feign-facing recipient lookup.
 */
@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/system/notice/recipients")
public class NoticeRecipientController {

    private final NoticeRecipientApi noticeRecipientApi;

    public NoticeRecipientController(NoticeRecipientApi noticeRecipientApi) {
        this.noticeRecipientApi = noticeRecipientApi;
    }

    @PostMapping
    public ApiResponse<List<NoticeRecipient>> list(@RequestBody NoticeRecipientRequest request) {
        return noticeRecipientApi.list(request);
    }
}
