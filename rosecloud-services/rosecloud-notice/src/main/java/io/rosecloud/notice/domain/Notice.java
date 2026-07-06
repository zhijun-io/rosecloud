package io.rosecloud.notice.domain;

import java.time.LocalDateTime;

/**
 * Domain view of a notice/announcement. ORM-free; mapped to/from {@code sys_notice}.
 * Targeting is resolved from the caller context at read time, so no per-user
 * delivery rows are pre-generated.
 */
public record Notice(Long id, String title, String content, Integer targetType,
                     Long targetTenantId, String targetRoleCode, Integer publishType,
                     LocalDateTime publishTime, LocalDateTime effectiveTime,
                     LocalDateTime expireTime, Integer status, Boolean needConfirm,
                     Long senderId, Long tenantId) {
}
