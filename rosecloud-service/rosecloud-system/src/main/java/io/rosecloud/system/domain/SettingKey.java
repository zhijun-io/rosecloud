package io.rosecloud.system.domain;

import java.time.LocalDateTime;

/** Domain view of a configuration key definition. */
public record SettingKey(String key, String name, String remark,
                         LocalDateTime updatedAt, Long updatedBy) {
}
