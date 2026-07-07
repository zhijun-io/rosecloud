package io.rosecloud.system.service.dto;

public record ConfigRequest(String configKey, String configValue, String description) {
}
