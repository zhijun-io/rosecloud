package io.rosecloud.notice.service.dto;

import io.rosecloud.notice.domain.Notice;

import java.time.LocalDateTime;

/** A notice as seen by a specific user, with their read/confirm state. */
public record MyNotice(Notice notice, boolean read, boolean confirmed,
                       LocalDateTime readTime, LocalDateTime confirmTime) {
}
