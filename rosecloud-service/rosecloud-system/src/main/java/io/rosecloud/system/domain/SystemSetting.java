package io.rosecloud.system.domain;

import java.time.LocalDateTime;

/** Domain view of a platform-level setting. */
public record SystemSetting(String key, String value,
                            LocalDateTime updatedAt, Long updatedBy) {
}
