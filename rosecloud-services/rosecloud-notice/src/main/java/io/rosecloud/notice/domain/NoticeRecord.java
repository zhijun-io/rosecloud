package io.rosecloud.notice.domain;

import java.time.LocalDateTime;

/**
 * Per-user read/confirm state for a notice. Created lazily on first interaction
 * (read or confirm); absence means the user has not yet opened it.
 */
public record NoticeRecord(Long id, Long noticeId, Long userId, Long tenantId,
                           LocalDateTime readTime, LocalDateTime confirmTime) {
}
