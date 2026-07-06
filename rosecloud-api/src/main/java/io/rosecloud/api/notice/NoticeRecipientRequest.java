package io.rosecloud.api.notice;

/** Request to resolve recipient contacts for a notice target. */
public record NoticeRecipientRequest(Integer targetType, Long targetTenantId, String targetRoleCode) {
}
