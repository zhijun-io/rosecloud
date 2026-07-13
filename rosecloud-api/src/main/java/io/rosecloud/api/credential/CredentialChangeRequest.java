package io.rosecloud.api.credential;

public record CredentialChangeRequest(String currentPassword, String newPassword) {
}
