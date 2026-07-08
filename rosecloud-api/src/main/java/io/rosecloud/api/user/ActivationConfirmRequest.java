package io.rosecloud.api.user;

public record ActivationConfirmRequest(String activateToken, String password) {
}
