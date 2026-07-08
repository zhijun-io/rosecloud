package io.rosecloud.notice.service.dto;

import java.time.LocalDateTime;

public record NoticePublishRequest(String title, String content, Integer targetType,
                                   String targetTenantId, String targetRoleCode, Integer publishType,
                                  LocalDateTime publishTime, LocalDateTime effectiveTime,
                                  LocalDateTime expireTime, Boolean needConfirm, Integer channels) {
}
