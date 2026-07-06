package io.rosecloud.system.domain;

import java.time.LocalDateTime;

/** Domain view of an async task tracked by the task center. ORM-free; mapped to/from {@code sys_task}. */
public record Task(Long id, String name, String type, TaskStatus status, Long tenantId,
                   String payload, String result, String error, int retryCount, int maxRetry,
                   LocalDateTime startedAt, LocalDateTime finishedAt, LocalDateTime createTime) {
}
