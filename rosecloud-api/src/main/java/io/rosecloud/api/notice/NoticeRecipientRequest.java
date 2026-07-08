package io.rosecloud.api.notice;

/** Request to resolve recipient contacts for a notice target. */
public record NoticeRecipientRequest(Integer targetType, String targetTenantId, String targetRoleCode,
                                     String targetUsername) {
}
