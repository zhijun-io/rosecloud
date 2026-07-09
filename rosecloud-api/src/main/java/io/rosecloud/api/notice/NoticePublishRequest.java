package io.rosecloud.api.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record NoticePublishRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content,
        @NotNull Integer targetType,
        String targetTenantId, String targetRoleCode, String targetUsername,
        Integer publishType,
        LocalDateTime publishTime, LocalDateTime effectiveTime,
        LocalDateTime expireTime, Boolean needConfirm, Integer channels,
        List<NoticeRecipient> recipients) {
}
