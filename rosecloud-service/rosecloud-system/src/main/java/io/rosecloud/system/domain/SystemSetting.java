package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasKey;
import io.rosecloud.common.core.model.HasUpdatedAt;
import io.rosecloud.common.core.model.HasUpdatedBy;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Value;

/** Domain view of a platform-level setting. */
@Value
@AllArgsConstructor
public final class SystemSetting implements HasKey, HasUpdatedAt, HasUpdatedBy {

    String key;
    String value;
    LocalDateTime updatedAt;
    Long updatedBy;
}
