package io.rosecloud.system.service.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
