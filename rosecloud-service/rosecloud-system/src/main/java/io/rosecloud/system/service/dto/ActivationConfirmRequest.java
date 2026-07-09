package io.rosecloud.system.service.dto;

public record ActivationConfirmRequest(String activateToken, String password) {
}
