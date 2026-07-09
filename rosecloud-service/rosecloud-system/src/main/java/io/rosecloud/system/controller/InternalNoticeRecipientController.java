package io.rosecloud.system.controller;

import io.rosecloud.api.notice.NoticeRecipient;
import io.rosecloud.api.notice.NoticeRecipientApi;
import io.rosecloud.api.notice.NoticeRecipientRequest;
import io.rosecloud.common.core.model.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Internal endpoint for the notice service to resolve recipient contacts. Not gateway-routed. */
@RestController
@RequestMapping("/internal/notice/recipients")
public class InternalNoticeRecipientController {

    private final NoticeRecipientApi noticeRecipientApi;

    public InternalNoticeRecipientController(NoticeRecipientApi noticeRecipientApi) {
        this.noticeRecipientApi = noticeRecipientApi;
    }

    @PostMapping
    public ApiResponse<List<NoticeRecipient>> list(@RequestBody NoticeRecipientRequest request) {
        return noticeRecipientApi.list(request);
    }
}
