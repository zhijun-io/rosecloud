package io.rosecloud.system.domain;

import io.rosecloud.common.core.model.HasKey;
import io.rosecloud.common.core.model.HasUpdatedAt;
import io.rosecloud.common.core.model.HasUpdatedBy;
import io.rosecloud.common.core.model.HasUserId;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Value;

/** Domain view of a per-user setting. */
@Value
@AllArgsConstructor
public final class UserSetting implements HasUserId, HasKey, HasUpdatedAt, HasUpdatedBy {

    Long userId;
    String key;
    String value;
    LocalDateTime updatedAt;
    Long updatedBy;
}
