package io.rosecloud.notice.domain;

import io.rosecloud.common.core.model.PageResult;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for notices and their per-user read/confirm records.
 * Implemented in the infrastructure layer; the service depends only on this
 * interface so persistence stays swappable.
 */
public interface NoticeRepository {

    Long insert(Notice notice);

    /** Scheduled notices now due (draft + scheduled + publishTime <= now). */
    List<Notice> findDueScheduled(LocalDateTime now);

    void markPublished(Long id);

    PageResult<Notice> page(long current, long size, String keyword);

    Optional<Notice> findById(Long id);

    PageResult<Notice> myNotices(long current, long size, String tenantId,
                                 Collection<String> roleCodes, LocalDateTime now);

    List<NoticeRecord> findRecords(Collection<Long> noticeIds, Long userId);

    void upsertRead(Long noticeId, Long userId, String tenantId, LocalDateTime now);

    void upsertConfirm(Long noticeId, Long userId, String tenantId, LocalDateTime now);
}
