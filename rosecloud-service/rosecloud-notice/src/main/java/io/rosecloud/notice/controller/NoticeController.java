package io.rosecloud.notice.controller;

import io.rosecloud.common.core.model.ApiResponse;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.core.model.ServiceMetadata;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.service.NoticeService;
import io.rosecloud.notice.service.dto.MyNotice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping(ServiceMetadata.API_PREFIX + "/notice/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PreAuthorize("hasAuthority('system:notice:publish')")
    @PostMapping
    public ApiResponse<Long> publish(@RequestBody @Valid NoticePublishRequest request) {
        return ApiResponse.ok(noticeService.publish(request));
    }

    @PreAuthorize("hasAuthority('system:notice:list')")
    @GetMapping
    public ApiResponse<PageResult<Notice>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword) {
        long[] bounds = clampPage(current, size);
        return ApiResponse.ok(noticeService.page(bounds[0], bounds[1], keyword));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ApiResponse<PageResult<MyNotice>> myNotices(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size) {
        long[] bounds = clampPage(current, size);
        return ApiResponse.ok(noticeService.myNotices(bounds[0], bounds[1]));
    }

    private static long[] clampPage(long current, long size) {
        long safeCurrent = current < 1 ? 1 : current;
        long safeSize = size < 1 ? 10 : Math.min(size, 100);
        return new long[]{safeCurrent, safeSize};
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/{id}")
    public ApiResponse<MyNotice> getMine(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.getMine(id));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        noticeService.markRead(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id) {
        noticeService.confirm(id);
        return ApiResponse.ok();
    }
}
