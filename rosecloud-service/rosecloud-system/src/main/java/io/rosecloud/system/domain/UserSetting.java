package io.rosecloud.system.domain;

import java.time.LocalDateTime;

/** Domain view of a per-user setting. */
public record UserSetting(Long userId, String key, String value,
                          LocalDateTime updatedAt, Long updatedBy) {
}
