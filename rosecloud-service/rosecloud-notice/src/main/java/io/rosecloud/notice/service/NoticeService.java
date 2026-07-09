package io.rosecloud.notice.service;

import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.service.dto.MyNotice;

public interface NoticeService {

    Long publish(NoticePublishRequest request);

    PageResult<Notice> page(long current, long size, String keyword);

    PageResult<MyNotice> myNotices(long current, long size);

    MyNotice getMine(Long id);

    void markRead(Long id);

    void confirm(Long id);

    int publishScheduledNotices();
}
