package io.rosecloud.system.service.dto;

public record TaskCreateRequest(String name, String type, String payload, Long tenantId) {
}
