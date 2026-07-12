package io.rosecloud.notice.domain;

import io.rosecloud.common.core.model.HasId;
import io.rosecloud.common.core.model.HasTenantId;
import io.rosecloud.common.core.model.HasUserId;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Per-user read/confirm state for a notice. Created lazily on first interaction
 * (read or confirm); absence means the user has not yet opened it.
 */
@Value
public class NoticeRecord implements HasId, HasUserId, HasTenantId {

    Long id;
    Long noticeId;
    Long userId;
    String tenantId;
    LocalDateTime readTime;
    LocalDateTime confirmTime;
    LocalDateTime createTime;
    Long createBy;
    LocalDateTime updateTime;
    Long updateBy;
}
