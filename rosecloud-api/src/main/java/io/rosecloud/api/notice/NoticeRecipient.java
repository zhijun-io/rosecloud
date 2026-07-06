package io.rosecloud.api.notice;

/** A resolved notice recipient's contact channels. */
public record NoticeRecipient(Long userId, String email, String phone) {
}
