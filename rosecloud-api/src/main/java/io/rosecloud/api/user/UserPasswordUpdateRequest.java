package io.rosecloud.api.user;

public record UserPasswordUpdateRequest(
        String currentPassword,
        String newPassword
) {
}
