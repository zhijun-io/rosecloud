package io.rosecloud.api.notice;

import java.util.List;
import java.time.LocalDateTime;

public record NoticePublishRequest(String title, String content, Integer targetType,
                                   String targetTenantId, String targetRoleCode, String targetUsername,
                                   Integer publishType,
                                   LocalDateTime publishTime, LocalDateTime effectiveTime,
                                   LocalDateTime expireTime, Boolean needConfirm, Integer channels,
                                   List<NoticeRecipient> recipients) {
}
